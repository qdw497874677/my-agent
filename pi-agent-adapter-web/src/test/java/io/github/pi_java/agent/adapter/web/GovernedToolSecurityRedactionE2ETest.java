package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {PiCloudServerApplication.class, GovernedToolSecurityRedactionE2ETest.SecurityRuntimeConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class GovernedToolSecurityRedactionE2ETest {

    static final String FAKE_SECRET = "PI_PHASE4_FAKE_SECRET_DO_NOT_LEAK";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void fakeSensitiveValuesAreAbsentFromRestEventsAuditAndPersistedPayloads() {
        RunOutcome outcome = runWithObjective("sensitive read-only tool " + FAKE_SECRET);

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertNoSecret(outcome.detail());
        assertNoSecret(outcome.events());
        assertNoSecret(stores.listByRun(outcome.run().runId(), 0, 500));
        assertNoSecret(stores.auditsForRun(outcome.run().runId()));
        assertThat(outcome.events().toString()).containsIgnoringCase("redacted");
        assertThat(stores.auditsForRun(outcome.run().runId()).toString()).containsIgnoringCase("redacted");
    }

    @Test
    void persistedEventsAndAuditCarryRedactionAndTruncationMetadataInsteadOfRawSecrets() {
        RunOutcome outcome = runWithObjective("sensitive read-only tool with oversized metadata");

        String events = stores.listByRun(outcome.run().runId(), 0, 500).toString();
        String audits = stores.auditsForRun(outcome.run().runId()).toString();
        assertThat(events).doesNotContain(FAKE_SECRET).containsIgnoringCase("redacted");
        assertThat(audits).doesNotContain(FAKE_SECRET).containsIgnoringCase("redacted").containsIgnoringCase("truncated");
    }

    @Test
    void exceptionMessagesExposeSafeCategoriesOnly() {
        RunOutcome outcome = runWithObjective("throw sensitive exception " + FAKE_SECRET);

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertNoSecret(outcome.detail());
        assertNoSecret(outcome.events());
        assertNoSecret(stores.auditsForRun(outcome.run().runId()));
        assertThat(outcome.events().toString()).contains("execution_failed").doesNotContain(FAKE_SECRET);
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-redaction-e2e", Map.of()), SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-redaction-e2e", Map.of()),
                RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        RunDetailResponse detail = get("/api/sessions/%s/runs/%s".formatted(session.sessionId(), run.runId()), RunDetailResponse.class);
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()),
                EventHistoryResponse.class);
        return new RunOutcome(session, run, status, detail, events);
    }

    private void assertNoSecret(Object value) {
        assertThat(value.toString()).doesNotContain(FAKE_SECRET);
    }

    private RunStatusResponse awaitTerminal(String sessionId, String runId) {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        RunStatusResponse current;
        do {
            current = get("/api/sessions/%s/runs/%s/status".formatted(sessionId, runId), RunStatusResponse.class);
            if (current.terminal()) {
                return current;
            }
            sleep(Duration.ofMillis(100));
        } while (System.nanoTime() < deadline);
        throw new AssertionError("run did not reach terminal state: " + runId);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private <T> T get(String path, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.add("X-Pi-Dev-Tenant", "tenant-redaction-e2e");
        headers.add("X-Pi-Dev-User", "user-redaction-e2e");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record RunOutcome(SessionResponse session, RunResponse run, RunStatusResponse status, RunDetailResponse detail,
                              EventHistoryResponse events) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityRuntimeConfiguration {
        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    model.script(new ModelResponse.ToolCallIntent(toolCall(context)));
                    model.script(new ModelResponse.FinalText("completed after security redaction"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-14T00:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        private static ToolCall toolCall(RunContext context) {
            String objective = context.input().toString().toLowerCase();
            String runId = context.workspaceScope().runId();
            Map<String, Object> arguments = objective.contains("throw")
                    ? Map.of("unexpectedSecret", "throw-" + FAKE_SECRET)
                    : Map.of("requestSecret", FAKE_SECRET, "note", "x".repeat(700));
            return new ToolCall("tool-call-" + runId, new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                    new io.github.pi_java.agent.domain.common.PlatformIds.StepId("planned-step-" + runId),
                    objective.contains("throw") ? "security.throwing" : "security.echo", arguments, Instant.parse("2026-06-14T00:00:00Z"));
        }

        @Bean
        @Primary
        io.github.pi_java.agent.app.port.tool.ToolRegistry toolRegistry() {
            io.github.pi_java.agent.domain.tool.ToolDescriptor echo = descriptor("security.echo");
            io.github.pi_java.agent.domain.tool.ToolDescriptor throwing = descriptor("security.throwing");
            return new io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry(List.of(
                    new io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry.ToolRegistration(echo, (request, cancellationToken) -> {
                        Map<String, Object> raw = Map.of("resultSecret", FAKE_SECRET, "safe", "visible", "large", "z".repeat(700));
                        return new io.github.pi_java.agent.domain.tool.ToolExecutionResult(request.toolCallId(), request.toolId(),
                                io.github.pi_java.agent.domain.tool.ToolExecutionStatus.SUCCESS, "returned redacted security payload",
                                java.util.Optional.empty(), Map.of(), raw, java.util.Set.of(), java.util.Optional.empty(), Duration.ZERO,
                                java.util.Optional.of(raw));
                    }),
                    new io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry.ToolRegistration(throwing, (request, cancellationToken) -> {
                        throw new IllegalStateException("boom " + FAKE_SECRET);
                    })
            ));
        }

        private static io.github.pi_java.agent.domain.tool.ToolDescriptor descriptor(String id) {
            return new io.github.pi_java.agent.domain.tool.ToolDescriptor(id, id, "Security redaction E2E tool",
                    new io.github.pi_java.agent.domain.tool.ToolSchema("https://json-schema.org/draft/2020-12/schema",
                            Map.of("type", "object", "additionalProperties", true), java.util.Set.of("requestSecret", "unexpectedSecret"), 2048),
                    java.util.Optional.empty(), new io.github.pi_java.agent.domain.tool.ToolProvenance(
                            io.github.pi_java.agent.domain.tool.ToolProvenance.SourceKind.BUILT_IN, "security-e2e", id, Map.of()),
                    "1.0.0", java.util.Set.of("tool:read"), io.github.pi_java.agent.domain.tool.ToolRiskLevel.LOW,
                    io.github.pi_java.agent.domain.tool.ToolSideEffect.READ_ONLY, Duration.ofSeconds(2), Map.of());
        }
    }
}
