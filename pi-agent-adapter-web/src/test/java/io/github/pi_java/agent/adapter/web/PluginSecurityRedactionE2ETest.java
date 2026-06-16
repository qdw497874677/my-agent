package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

@SpringBootTest(classes = {PiCloudServerApplication.class, PluginSecurityRedactionE2ETest.PluginRedactionConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class PluginSecurityRedactionE2ETest {
    static final String FAKE_SECRET = "PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK";
    static final String RAW_PATH = "/var/secret/plugins/fake-plugin.jar";
    static final String RAW_EXCEPTION = "raw plugin exception body text";
    static final String ENV_NAME = "PI_PHASE8_PLUGIN_TOKEN";
    static final String ENV_VALUE = "env-value-do-not-leak";
    static final String SENSITIVE_METADATA = "plugin-metadata-secret";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void rawPluginSecretsAreAbsentFromRestEventsAuditAdminAndNormalizedErrors() {
        RunOutcome outcome = runWithObjective("plugin redaction failure " + FAKE_SECRET + " " + RAW_PATH + " " + ENV_NAME);

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertNoRawPluginSensitiveText(outcome.detail());
        assertNoRawPluginSensitiveText(outcome.events());
        assertNoRawPluginSensitiveText(stores.listByRun(outcome.run().runId(), 0, 500));
        assertNoRawPluginSensitiveText(stores.auditsForRun(outcome.run().runId()));
        assertNoRawPluginSensitiveText(get("/api/admin/governance/plugins", String.class));
        assertNoRawPluginSensitiveText(get("/api/tools", String.class));
        assertThat(outcome.events().toString()).contains("PLUGIN_TOOL_ERROR").containsIgnoringCase("redacted");
    }

    @Test
    void pluginUiFixtureTextDoesNotExposeRawPluginSecretsOrPaths() {
        WebConsoleE2EFixtureConfiguration fixture = new WebConsoleE2EFixtureConfiguration();

        assertNoRawPluginSensitiveText(fixture.pluginFixtureText());
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-plugin-redaction", Map.of()), SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-plugin-redaction", Map.of()),
                RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        RunDetailResponse detail = get("/api/sessions/%s/runs/%s".formatted(session.sessionId(), run.runId()), RunDetailResponse.class);
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()),
                EventHistoryResponse.class);
        return new RunOutcome(session, run, status, detail, events);
    }

    private void assertNoRawPluginSensitiveText(Object value) {
        assertThat(value.toString()).doesNotContain(FAKE_SECRET, RAW_PATH, RAW_EXCEPTION, ENV_NAME, ENV_VALUE, SENSITIVE_METADATA);
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
        headers.add("X-Pi-Dev-Tenant", "tenant-plugin-redaction");
        headers.add("X-Pi-Dev-User", "user-plugin-redaction");
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
    static class PluginRedactionConfiguration {
        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    model.script(new ModelResponse.ToolCallIntent(toolCall(context)));
                    model.script(new ModelResponse.FinalText("completed after plugin redaction"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-16T11:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        private static ToolCall toolCall(RunContext context) {
            String runId = context.workspaceScope().runId();
            return new ToolCall("plugin-redaction-call-" + runId,
                    new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                    new io.github.pi_java.agent.domain.common.PlatformIds.StepId("plugin-redaction-step-" + runId),
                    "plugin.secret.throw", Map.of("secret", FAKE_SECRET, "path", RAW_PATH, "env", ENV_NAME),
                    Instant.parse("2026-06-16T11:00:00Z"));
        }

        @Bean
        @Primary
        ToolRegistry toolRegistry() {
            ToolDescriptor descriptor = new ToolDescriptor("plugin.secret.throw", "Plugin secret throw", "Plugin redaction tool",
                    new ToolSchema("json-schema", Map.of("type", "object", "additionalProperties", true), Set.of("secret", "path", "env"), 2048),
                    Optional.empty(), new ToolProvenance(ToolProvenance.SourceKind.PLUGIN, "fake-plugin", "capability:secret",
                    Map.of("plugin.sourcePath", "fake-plugin.jar", "plugin.metadataSummary", "redacted")),
                    "1.0.0", Set.of("tool:plugin"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(2),
                    Map.of("plugin.metadataSummary", "redacted"));
            return new io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry(List.of(
                    new io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry.ToolRegistration(descriptor, (request, token) ->
                            new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.FAILED, "PLUGIN_TOOL_ERROR",
                                    Optional.of("PLUGIN_TOOL_ERROR"), Map.of("error", "redacted plugin failure"), Map.of(), Set.of(),
                                    Optional.empty(), Duration.ZERO, Optional.empty()))));
        }

        @Bean
        @Primary
        PluginGovernanceCatalog pluginGovernanceCatalog() {
            return new PluginGovernedToolE2ETest.PluginE2EConfiguration().pluginGovernanceCatalog();
        }

        @Bean
        @Primary
        ToolPolicyEvaluator toolPolicyEvaluator() {
            return request -> new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.ALLOW, "plugin redaction allowed",
                    "plugin-redaction-policy", false, Optional.empty(), Optional.empty(), Map.of());
        }

        @Bean
        @Primary
        AgentDefinition agentDefinition() {
            return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "Plugin redaction E2E runtime",
                    "fake-model", Set.of("tool:plugin"), Set.of("plugin-redaction-policy"), new RuntimeLimits(Duration.ofSeconds(30), 8, 8),
                    Set.of(InteractionMode.CHAT, InteractionMode.TASK), "test-workspace-policy", "test-output-policy");
        }
    }
}
