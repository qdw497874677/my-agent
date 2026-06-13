package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.ModelClient;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolInvoker;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePortsContractTest {

    @Test
    void runtime_ports_compile_with_only_domain_types() {
        CancellationToken cancellationToken = new CancellationToken();
        RunContext context = new RunContext(
                new AgentDefinition(
                        new AgentId("agent-1"),
                        "General Agent",
                        "Use tools when needed.",
                        "fake-model",
                        Set.of("workspace"),
                        Set.of("default"),
                        new RuntimeLimits(Duration.ofMinutes(1), 4, 2),
                        Set.of(InteractionMode.CHAT),
                        "workspace-policy",
                        "output-policy"),
                new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty(), List.of()),
                new WorkspaceScope("tenant-1", "user-1", "session-1", "run-1", "workspace-1", Set.of(), Set.of()),
                new RuntimeLimits(Duration.ofMinutes(1), 4, 2),
                cancellationToken,
                "trace-1",
                Instant.parse("2026-06-13T00:00:00Z"));

        AgentRuntime runtime = new AgentRuntime() {
            @Override
            public RunHandle start(RunContext context) {
                return new RunHandle("run-1", RunStatus.RUNNING, Optional.empty());
            }

            @Override
            public void cancel(String runId, String reason) {
                cancellationToken.cancel(reason);
            }
        };
        ModelClient modelClient = (request, token) -> new ModelResponse.FinalText("done");
        ToolInvoker toolInvoker = (ToolCall toolCall, RunContext runContext, CancellationToken token) -> null;

        assertThat(runtime.start(context).status()).isEqualTo(RunStatus.RUNNING);
        assertThat(modelClient.next(new ModelRequest(context, List.of()), cancellationToken))
                .isInstanceOf(ModelResponse.FinalText.class);
        assertThat(toolInvoker).isNotNull();
        assertThat(PolicyDecision.REQUIRE_APPROVAL.isTerminalBlock()).isFalse();
        assertThat(PolicyDecision.REQUIRE_SANDBOX.requiresHumanOrSandboxGate()).isTrue();
    }

    @Test
    void cancellation_token_can_be_cancelled_and_reports_reason() {
        CancellationToken token = new CancellationToken();

        assertThat(token.isCancellationRequested()).isFalse();

        token.cancel("user requested stop");

        assertThat(token.isCancellationRequested()).isTrue();
        assertThat(token.reason()).contains("user requested stop");
    }
}
