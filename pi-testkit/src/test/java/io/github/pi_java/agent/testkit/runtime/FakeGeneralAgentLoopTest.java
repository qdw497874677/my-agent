package io.github.pi_java.agent.testkit.runtime;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolResult;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.EventCollector;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.FakeToolInvoker;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FakeGeneralAgentLoopTest {

    @Test
    void model_tool_model_path_emits_ordered_terminal_events() {
        EventCollector events = new EventCollector();
        ToolCall toolCall = new ToolCall("tool-call-1", nullRunId(), nullStepId(), "lookup", Map.of("q", "pi"), START);
        FakeModelClient model = new FakeModelClient()
                .script(new ModelResponse.ToolCallIntent(toolCall))
                .script(new ModelResponse.FinalText("done"));
        FakeToolInvoker tools = new FakeToolInvoker().register("lookup", new ToolResult("tool-call-1", true, "found pi", null, START));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, tools, FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.SUCCEEDED);
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
        assertEveryEventHasVisibilityAndRedaction(events.events());
        assertThat(events.types()).containsSequence(
                RunEventType.RUN_CREATED,
                RunEventType.RUN_STARTED,
                RunEventType.MODEL_REQUESTED,
                RunEventType.TOOL_PROPOSED,
                RunEventType.TOOL_POLICY_DECIDED,
                RunEventType.TOOL_STARTED,
                RunEventType.TOOL_COMPLETED,
                RunEventType.MODEL_REQUESTED,
                RunEventType.MODEL_DELTA,
                RunEventType.RUN_COMPLETED);
        assertThat(tools.invocations()).hasSize(1);
    }

    @Test
    void denied_tool_call_is_gateway_routed_and_skips_executor_side_effect() {
        EventCollector events = new EventCollector();
        ToolCall toolCall = new ToolCall("tool-call-1", nullRunId(), nullStepId(), "lookup", Map.of("q", "pi"), START);
        FakeModelClient model = new FakeModelClient().script(new ModelResponse.ToolCallIntent(toolCall));
        FakeToolInvoker tools = new FakeToolInvoker().register("lookup", new ToolResult("tool-call-1", true, "found pi", null, START));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, tools, new FakePolicy(PolicyDecision.DENY), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.POLICY_BLOCKED);
        assertThat(tools.invocations()).isEmpty();
        assertThat(events.types()).contains(RunEventType.TOOL_PROPOSED, RunEventType.TOOL_POLICY_DECIDED,
                RunEventType.TOOL_DENIED, RunEventType.RUN_POLICY_BLOCKED);
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
    }

    @Test
    void approval_required_tool_call_is_gateway_routed_and_skips_executor_side_effect() {
        EventCollector events = new EventCollector();
        ToolCall toolCall = new ToolCall("tool-call-1", nullRunId(), nullStepId(), "lookup", Map.of("q", "pi"), START);
        FakeModelClient model = new FakeModelClient().script(new ModelResponse.ToolCallIntent(toolCall));
        FakeToolInvoker tools = new FakeToolInvoker().register("lookup", new ToolResult("tool-call-1", true, "found pi", null, START));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, tools, new FakePolicy(PolicyDecision.REQUIRE_APPROVAL), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.POLICY_BLOCKED);
        assertThat(tools.invocations()).isEmpty();
        assertThat(events.types()).contains(RunEventType.TOOL_PROPOSED, RunEventType.TOOL_POLICY_DECIDED,
                RunEventType.TOOL_PREVIEW_GENERATED, RunEventType.TOOL_APPROVAL_REQUIRED, RunEventType.RUN_POLICY_BLOCKED);
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
    }

    @Test
    void general_agent_loop_source_does_not_directly_invoke_legacy_tool_invoker() throws Exception {
        Path sourcePath = Path.of("src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java");
        }
        String source = Files.readString(sourcePath);

        assertThat(source).contains("toolExecutionGateway.execute");
        assertThat(source).doesNotContain("toolInvoker.invoke");
    }

    @Test
    void max_step_budget_fails_with_single_terminal_event() {
        EventCollector events = new EventCollector();
        FakeModelClient model = new FakeModelClient()
                .script(new ModelResponse.ToolCallIntent(new ToolCall("tool-call-1", nullRunId(), nullStepId(), "lookup", Map.of(), START)))
                .script(new ModelResponse.FinalText("too late"));
        FakeToolInvoker tools = new FakeToolInvoker().register("lookup", new ToolResult("tool-call-1", true, "ok", null, START));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, tools, FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 1, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.FAILED);
        assertThat(handle.failureSummary()).isPresent();
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
        assertEveryEventHasVisibilityAndRedaction(events.events());
        assertThat(events.last().type()).isEqualTo(RunEventType.RUN_FAILED);
    }

    @Test
    void pre_cancelled_token_results_in_cancelled_terminal_event() {
        EventCollector events = new EventCollector();
        CancellationToken token = new CancellationToken();
        token.cancel("user stopped run");
        GeneralAgentLoop loop = new GeneralAgentLoop(new FakeModelClient(), new FakeToolInvoker(), FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), token));

        assertThat(handle.status()).isEqualTo(RunStatus.CANCELLED);
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
        assertEveryEventHasVisibilityAndRedaction(events.events());
        assertThat(events.last().type()).isEqualTo(RunEventType.RUN_CANCELLED);
    }

    @Test
    void cancellation_before_tool_call_skips_tool_and_emits_cancelled_terminal_event() {
        EventCollector events = new EventCollector();
        CancellationToken token = new CancellationToken();
        ToolCall toolCall = new ToolCall("tool-call-1", nullRunId(), nullStepId(), "lookup", Map.of(), START);
        FakeModelClient model = new FakeModelClient().scriptThenCancel(new ModelResponse.ToolCallIntent(toolCall), token, "cancel before tool");
        FakeToolInvoker tools = new FakeToolInvoker().register("lookup", new ToolResult("tool-call-1", true, "ok", null, START));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, tools, FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), token));

        assertThat(handle.status()).isEqualTo(RunStatus.CANCELLED);
        assertThat(tools.invocations()).isEmpty();
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
        assertEveryEventHasVisibilityAndRedaction(events.events());
    }

    @Test
    void expired_deadline_fails_with_normalized_recoverable_failure_summary() {
        EventCollector events = new EventCollector();
        GeneralAgentLoop loop = new GeneralAgentLoop(new FakeModelClient(), new FakeToolInvoker(), FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START.plusSeconds(5)));

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofSeconds(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.FAILED);
        assertThat(handle.failureSummary()).isPresent();
        assertThat(handle.failureSummary().orElseThrow().error().retryable()).isFalse();
        assertThat(handle.failureSummary().orElseThrow().error().recoverable()).isTrue();
        assertThat(handle.failureSummary().orElseThrow().error().userActionRequired()).isFalse();
        assertMonotonicSequences(events.events());
        assertExactlyOneTerminalEventLast(events.events());
        assertEveryEventHasVisibilityAndRedaction(events.events());
    }

    private static void assertMonotonicSequences(List<RunEvent> events) {
        assertThat(events).isNotEmpty();
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).sequence()).isEqualTo(i + 1L);
        }
    }

    private static void assertExactlyOneTerminalEventLast(List<RunEvent> events) {
        assertThat(events.stream().filter(event -> EventCollector.isTerminal(event.type())).count()).isEqualTo(1);
        assertThat(EventCollector.isTerminal(events.get(events.size() - 1).type())).isTrue();
    }

    private static void assertEveryEventHasVisibilityAndRedaction(List<RunEvent> events) {
        assertThat(events).allSatisfy(event -> {
            assertThat(event.visibility()).as("visibility for %s", event.type()).isNotNull();
            assertThat(event.redaction()).as("redaction for %s", event.type()).isNotNull();
            assertThat(event.redaction().redactedFields()).as("redacted fields for %s", event.type()).isNotNull();
            assertThat(event.redaction().policyRef()).as("redaction policy for %s", event.type()).isNotBlank();
        });
    }

    private static final Instant START = Instant.parse("2026-06-13T00:00:00Z");

    private static RunContext context(RuntimeLimits limits, CancellationToken token) {
        return new RunContext(
                new AgentDefinition(new AgentId("agent-1"), "General Agent", "Use tools.", "fake-model",
                        Set.of("workspace"), Set.of("default"), limits, Set.of(InteractionMode.CHAT),
                        "workspace-policy", "output-policy"),
                new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty(), List.of()),
                new WorkspaceScope("tenant-1", "user-1", "session-1", "run-1", "workspace-1", Set.of(), Set.of()),
                limits,
                token,
                "trace-1",
                START);
    }

    private static io.github.pi_java.agent.domain.common.PlatformIds.RunId nullRunId() {
        return new io.github.pi_java.agent.domain.common.PlatformIds.RunId("run-1");
    }

    private static io.github.pi_java.agent.domain.common.PlatformIds.StepId nullStepId() {
        return new io.github.pi_java.agent.domain.common.PlatformIds.StepId("step-1");
    }
}
