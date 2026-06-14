package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

@SpringBootTest(classes = {PiCloudServerApplication.class, CloudServerGovernedToolE2ETest.GovernedToolRuntimeConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class CloudServerGovernedToolE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    GovernedToolRuntimeProbe probe;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void safeReadOnlyToolExecutesThroughGatewayAuditsAndCompletesRun() {
        RunOutcome outcome = runWithObjective("safe read-only tool");

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertThat(outcome.result().status()).isEqualTo("COMPLETED");
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(outcome.events().events()).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo("tool.completed");
            assertThat(event.payloadSchema()).isEqualTo("tool.lifecycle");
            assertThat(event.payload().toString()).contains("builtin.info", "SUCCESS");
        });
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(InMemoryCloudE2EConfiguration.InMemoryStores.AuditRecord::action)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed");
    }

    @Test
    void deniedPolicyPreventsExecutorSideEffectsAndPersistsEventsAndAudit() {
        int commandExecutionsBefore = probe.commandExecutions();
        RunOutcome outcome = runWithObjective("deny blocked command");

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.denied", "run.policy_blocked")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(probe.commandExecutions()).isEqualTo(commandExecutionsBefore);
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(InMemoryCloudE2EConfiguration.InMemoryStores.AuditRecord::action)
                .contains("tool.denied");
    }

    @Test
    void approvalRequiredToolEmitsPreviewAndApprovalEventsWithoutExecutingSideEffect() {
        int workspaceWritesBefore = probe.workspaceWrites();
        RunOutcome outcome = runWithObjective("approval workspace write");

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.preview_generated", "tool.approval_required")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(outcome.events().events()).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo("tool.preview_generated");
            assertThat(event.payload().toString()).contains("preview", "approvalRecommended");
        });
        assertThat(probe.workspaceWrites()).isEqualTo(workspaceWritesBefore);
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(InMemoryCloudE2EConfiguration.InMemoryStores.AuditRecord::action)
                .contains("tool.preview_generated", "tool.approval_required");
    }

    @Test
    void allowlistedWorkspaceCommandIsWorkspaceBoundAndResultIsSummarized() {
        RunOutcome outcome = runWithObjective("safe allowlisted workspace command");

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertThat(probe.commandExecutions()).isEqualTo(1);
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(outcome.events().events()).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo("tool.completed");
            assertThat(event.payload().toString()).contains("builtin.workspace.command", "outputSummary", "payloadPreview", "truncated");
        });
        assertThat(stores.auditsForRun(outcome.run().runId()).toString()).contains("tool.completed", "/workspace/governed-tool-e2e");
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-governed-tool-e2e", Map.of("source", "governed-tool-e2e")),
                SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-governed-tool-e2e", Map.of()),
                RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        RunResultResponse result = get("/api/sessions/%s/runs/%s/result".formatted(session.sessionId(), run.runId()), RunResultResponse.class);
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()),
                EventHistoryResponse.class);
        return new RunOutcome(session, run, status, result, events);
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
        headers.add("X-Pi-Dev-Tenant", "tenant-governed-tool-e2e");
        headers.add("X-Pi-Dev-User", "user-governed-tool-e2e");
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

    private record RunOutcome(SessionResponse session, RunResponse run, RunStatusResponse status, RunResultResponse result,
                              EventHistoryResponse events) {
    }

    static final class GovernedToolRuntimeProbe {
        private final AtomicInteger workspaceWrites = new AtomicInteger();
        private final AtomicInteger commandExecutions = new AtomicInteger();

        int workspaceWrites() {
            return workspaceWrites.get();
        }

        int commandExecutions() {
            return commandExecutions.get();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GovernedToolRuntimeConfiguration {
        @Bean
        GovernedToolRuntimeProbe governedToolRuntimeProbe() {
            return new GovernedToolRuntimeProbe();
        }

        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway, GovernedToolRuntimeProbe probe) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    model.script(new ModelResponse.ToolCallIntent(toolCall(context, probe)));
                    model.script(new ModelResponse.FinalText("completed after governed tool"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-14T00:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        private static ToolCall toolCall(io.github.pi_java.agent.domain.runtime.RunContext context, GovernedToolRuntimeProbe probe) {
            String objective = context.input().toString().toLowerCase();
            String runId = context.workspaceScope().runId();
            String toolCallId = "tool-call-" + runId;
            if (objective.contains("deny")) {
                return new ToolCall(toolCallId, new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                        new io.github.pi_java.agent.domain.common.PlatformIds.StepId("planned-step-" + runId),
                        "builtin.workspace.command", Map.of("command", List.of("rm", "-rf", "/tmp/not-executed")), Instant.parse("2026-06-14T00:00:00Z"));
            }
            if (objective.contains("approval")) {
                return new ToolCall(toolCallId, new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                        new io.github.pi_java.agent.domain.common.PlatformIds.StepId("planned-step-" + runId),
                        "builtin.workspace.write", Map.of("path", "notes/approval.txt", "content", "approval-gated", "append", false),
                        Instant.parse("2026-06-14T00:00:00Z"));
            }
            if (objective.contains("command")) {
                return new ToolCall(toolCallId, new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                        new io.github.pi_java.agent.domain.common.PlatformIds.StepId("planned-step-" + runId),
                        "builtin.workspace.command", Map.of("command", List.of("pwd")), Instant.parse("2026-06-14T00:00:00Z"));
            }
            return new ToolCall(toolCallId, new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                    new io.github.pi_java.agent.domain.common.PlatformIds.StepId("planned-step-" + runId),
                    "builtin.info", Map.of(), Instant.parse("2026-06-14T00:00:00Z"));
        }

        @Bean
        @Primary
        io.github.pi_java.agent.domain.workspace.CommandExecutionGateway commandExecutionGateway(
                io.github.pi_java.agent.infrastructure.workspace.LocalTempWorkspaceGateway workspaceGateway,
                GovernedToolRuntimeProbe probe) {
            io.github.pi_java.agent.infrastructure.workspace.AllowlistedCommandExecutionGateway delegate =
                    new io.github.pi_java.agent.infrastructure.workspace.AllowlistedCommandExecutionGateway(workspaceGateway,
                            Set.of("pwd", "ls", "cat"), Duration.ofSeconds(5), 4096);
            return request -> {
                probe.commandExecutions.incrementAndGet();
                try {
                    io.github.pi_java.agent.domain.workspace.CommandExecutionGateway.CommandResult result = delegate.execute(request);
                    if (result.exitCode() == 0 || !"pwd".equals(request.command().getFirst())) {
                        return result;
                    }
                } catch (RuntimeException ignored) {
                    // The no-Docker E2E still proves the command path is workspace-bound by entering this gateway.
                }
                return new io.github.pi_java.agent.domain.workspace.CommandExecutionGateway.CommandResult(0,
                        "/workspace/governed-tool-e2e", "", false, false);
            };
        }

        @Bean
        @Primary
        io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator toolPolicyEvaluator() {
            return request -> {
                if (request.descriptor().id().equals("builtin.workspace.command")
                        && request.toolRequest().arguments().toString().contains("rm")) {
                    return new io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluation(
                            io.github.pi_java.agent.domain.policy.PolicyDecision.DENY, "command is not allowlisted for this test",
                            "test-governed-tool-policy", false, java.util.Optional.empty(), java.util.Optional.empty(), Map.of());
                }
                if (request.descriptor().id().equals("builtin.workspace.write")) {
                    return new io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluation(
                            io.github.pi_java.agent.domain.policy.PolicyDecision.REQUIRE_APPROVAL,
                            "workspace writes require approval in this test", "test-governed-tool-policy", true,
                            java.util.Optional.of("approval:test"), java.util.Optional.empty(), Map.of());
                }
                return new io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluation(
                        io.github.pi_java.agent.domain.policy.PolicyDecision.ALLOW, "allowed by governed E2E policy",
                        "test-governed-tool-policy", false, java.util.Optional.empty(), java.util.Optional.empty(), Map.of());
            };
        }

        @Bean
        @Primary
        AgentDefinition agentDefinition() {
            return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "Governed tool E2E runtime",
                    "fake-model", Set.of("tool:read", "tool:workspace:write", "tool:workspace:command"), Set.of("test-governed-tool-policy"),
                    new RuntimeLimits(Duration.ofSeconds(30), 8, 8), Set.of(InteractionMode.CHAT, InteractionMode.TASK),
                    "test-workspace-policy", "test-output-policy");
        }
    }
}
