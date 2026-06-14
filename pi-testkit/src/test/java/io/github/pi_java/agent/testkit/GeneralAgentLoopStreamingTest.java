package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolResult;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralAgentLoopStreamingTest {
    private static final Instant START = Instant.parse("2026-06-14T00:00:00Z");

    @Test
    void streaming_text_chunks_emit_ordered_model_delta_events_before_completion() {
        EventCollector events = new EventCollector();
        ModelUsage usage = new ModelUsage(3, 5, 8);
        GeneralAgentLoop loop = loop(new FakeStreamingModelClient()
                .text("hel")
                .text("lo")
                .finish(ModelFinishReason.STOP, usage), events, new FakeToolInvoker());

        RunHandle handle = loop.start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.SUCCEEDED);
        events.assertMonotonicSequences();
        events.assertExactlyOneTerminalEventLast();
        assertThat(events.types()).containsSequence(
                RunEventType.MODEL_REQUESTED,
                RunEventType.MODEL_DELTA,
                RunEventType.MODEL_DELTA,
                RunEventType.MODEL_DELTA,
                RunEventType.RUN_COMPLETED);
        assertThat(modelDeltas(events)).extracting(RunEventPayload.ModelDeltaPayload::textDelta)
                .containsExactly("hel", "lo", "");
        RunEventPayload.ModelDeltaPayload metadata = modelDeltas(events).get(2);
        assertThat(metadata.finishReason()).isEqualTo(ModelFinishReason.STOP);
        assertThat(metadata.usage()).isEqualTo(usage);
        assertThat(metadata.modelRef()).isEqualTo("fake-provider:fake-model");
    }

    @Test
    void complete_tool_call_intent_flows_through_existing_policy_and_tool_path() {
        EventCollector events = new EventCollector();
        ToolCall toolCall = new ToolCall("tool-call-1", new RunId("run-1"), new StepId("step-1"),
                "lookup", Map.of("query", "pi"), START);
        FakeStreamingModelClient model = new FakeStreamingModelClient()
                .toolCall(toolCall)
                .nextStream()
                .text("done")
                .finish(ModelFinishReason.STOP);
        FakeToolInvoker tools = new FakeToolInvoker()
                .register("lookup", new ToolResult("tool-call-1", true, "found", null, START));

        RunHandle handle = loop(model, events, tools)
                .start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.SUCCEEDED);
        events.assertExactlyOneTerminalEventLast();
        assertThat(events.types()).containsSequence(
                RunEventType.TOOL_PROPOSED,
                RunEventType.POLICY_DECIDED,
                RunEventType.TOOL_COMPLETED,
                RunEventType.MODEL_REQUESTED,
                RunEventType.MODEL_DELTA,
                RunEventType.MODEL_DELTA,
                RunEventType.RUN_COMPLETED);
        assertThat(tools.invocations()).hasSize(1);
    }

    @Test
    void cancellation_during_streaming_returns_cancelled_and_terminal_event_last() {
        EventCollector events = new EventCollector();
        CancellationToken token = new CancellationToken();
        FakeStreamingModelClient model = new FakeStreamingModelClient()
                .text("before")
                .thenCancel(token, "user stopped stream")
                .text("after");

        RunHandle handle = loop(model, events, new FakeToolInvoker())
                .start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), token));

        assertThat(handle.status()).isEqualTo(RunStatus.CANCELLED);
        events.assertExactlyOneTerminalEventLast();
        assertThat(events.last().type()).isEqualTo(RunEventType.RUN_CANCELLED);
        assertThat(modelDeltas(events)).extracting(RunEventPayload.ModelDeltaPayload::textDelta).containsExactly("before");
    }

    @Test
    void provider_error_chunk_maps_to_model_failure_without_secret_content() {
        EventCollector events = new EventCollector();
        FakeStreamingModelClient model = new FakeStreamingModelClient()
                .providerError(new ProviderErrorSummary(
                        new PiError(PiError.Category.MODEL, "provider.rate_limited", PiError.Severity.ERROR,
                                EventVisibility.USER, true, true, false),
                        "rate_limited",
                        "Provider rate limit exceeded",
                        429,
                        true,
                        true,
                        false));

        RunHandle handle = loop(model, events, new FakeToolInvoker())
                .start(context(new RuntimeLimits(Duration.ofMinutes(1), 4, 2), new CancellationToken()));

        assertThat(handle.status()).isEqualTo(RunStatus.FAILED);
        assertThat(handle.failureSummary()).isPresent();
        assertThat(handle.failureSummary().orElseThrow().error().category()).isEqualTo(PiError.Category.MODEL);
        assertThat(handle.failureSummary().orElseThrow().message()).doesNotContain("Authorization", "api_key", "sk-");
        events.assertExactlyOneTerminalEventLast();
        assertThat(events.last().type()).isEqualTo(RunEventType.RUN_FAILED);
    }

    private static GeneralAgentLoop loop(FakeStreamingModelClient model, EventCollector events, FakeToolInvoker tools) {
        return new GeneralAgentLoop(model, tools, FakePolicy.allow(), events,
                new DeterministicIds(), new DeterministicClock(START));
    }

    private static List<RunEventPayload.ModelDeltaPayload> modelDeltas(EventCollector events) {
        return events.events().stream()
                .filter(event -> event.type() == RunEventType.MODEL_DELTA)
                .map(RunEvent::payload)
                .map(RunEventPayload.ModelDeltaPayload.class::cast)
                .toList();
    }

    private static RunContext context(RuntimeLimits limits, CancellationToken token) {
        return new RunContext(
                new AgentDefinition(new AgentId("agent-1"), "General Agent", "Use tools.", "fake-provider:fake-model",
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
}
