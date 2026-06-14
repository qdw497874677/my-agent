package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FakeStreamingModelClientTest {
    private static final Instant START = Instant.parse("2026-06-14T00:00:00Z");

    @Test
    void emits_scripted_text_tool_usage_finish_and_error_chunks_deterministically() {
        ToolCall toolCall = new ToolCall("call-1", new RunId("run-1"), new StepId("step-1"),
                "lookup", Map.of("query", "pi"), START);
        ModelUsage usage = new ModelUsage(10, 20, 30);
        ProviderErrorSummary error = modelError("Provider overloaded");
        List<ModelStreamChunk> chunks = new ArrayList<>();
        FakeStreamingModelClient client = new FakeStreamingModelClient()
                .providerId("fake-openai")
                .modelId("fake-gpt")
                .text("hel")
                .text("lo")
                .toolCall(toolCall)
                .usage(usage)
                .finish(ModelFinishReason.TOOL_CALLS, usage)
                .providerError(error);

        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(chunks).extracting(ModelStreamChunk::sequence).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(chunks).extracting(ModelStreamChunk::providerId).containsOnly("fake-openai");
        assertThat(chunks).extracting(ModelStreamChunk::modelId).containsOnly("fake-gpt");
        assertThat(chunks).extracting(ModelStreamChunk::modelRef).containsOnly("fake-openai:fake-gpt");
        assertThat(chunks.get(0)).isInstanceOfSatisfying(ModelStreamChunk.TextDelta.class,
                chunk -> assertThat(chunk.textDelta()).isEqualTo("hel"));
        assertThat(chunks.get(2)).isInstanceOfSatisfying(ModelStreamChunk.ToolCallIntent.class,
                chunk -> assertThat(chunk.toolCall()).isEqualTo(toolCall));
        assertThat(chunks.get(3)).isInstanceOfSatisfying(ModelStreamChunk.Usage.class,
                chunk -> assertThat(chunk.usage()).isEqualTo(usage));
        assertThat(chunks.get(4)).isInstanceOfSatisfying(ModelStreamChunk.Finished.class, chunk -> {
            assertThat(chunk.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
            assertThat(chunk.usage()).isEqualTo(usage);
        });
        assertThat(chunks.get(5)).isInstanceOfSatisfying(ModelStreamChunk.ProviderError.class,
                chunk -> assertThat(chunk.errorSummary()).isEqualTo(error));
    }

    @Test
    void stops_streaming_when_scripted_cancellation_is_requested() {
        CancellationToken token = new CancellationToken();
        List<ModelStreamChunk> chunks = new ArrayList<>();
        FakeStreamingModelClient client = new FakeStreamingModelClient()
                .text("before")
                .thenCancel(token, "user cancelled")
                .text("after");

        client.stream(request(), token, chunks::add);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isInstanceOfSatisfying(ModelStreamChunk.TextDelta.class,
                chunk -> assertThat(chunk.textDelta()).isEqualTo("before"));
        assertThat(chunks.get(1)).isInstanceOfSatisfying(ModelStreamChunk.Cancelled.class,
                chunk -> assertThat(chunk.reason()).isEqualTo("user cancelled"));
    }

    @Test
    void can_script_explicit_delayed_chunks_without_sleeping_or_network() {
        List<ModelStreamChunk> chunks = new ArrayList<>();
        FakeStreamingModelClient client = new FakeStreamingModelClient()
                .delayed(Duration.ofMillis(42), new ModelStreamChunk.TextDelta(
                        "fake-provider", "fake-model", "fake-openai:fake-gpt", 99, Duration.ZERO, "slow"));

        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().latency()).isEqualTo(Duration.ofMillis(42));
    }

    private static ModelRequest request() {
        RuntimeLimits limits = new RuntimeLimits(Duration.ofMinutes(1), 4, 2);
        return new ModelRequest(new RunContext(
                new AgentDefinition(new AgentId("agent-1"), "General Agent", "Use tools.", "fake-openai:fake-gpt",
                        Set.of("workspace"), Set.of("default"), limits, Set.of(InteractionMode.CHAT),
                        "workspace-policy", "output-policy"),
                new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty(), List.of()),
                new WorkspaceScope("tenant-1", "user-1", "session-1", "run-1", "workspace-1", Set.of(), Set.of()),
                limits,
                new CancellationToken(),
                "trace-1",
                START),
                List.of());
    }

    private static ProviderErrorSummary modelError(String message) {
        return new ProviderErrorSummary(
                new PiError(PiError.Category.MODEL, "provider.error", PiError.Severity.ERROR,
                        EventVisibility.USER, false, true, false),
                "provider.error",
                message,
                500,
                false,
                true,
                false);
    }
}
