package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolCall;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelStreamingContractsTest {

    @Test
    void streaming_chunks_are_provider_neutral_and_cover_text_tool_usage_finish_and_errors() {
        ModelUsage usage = new ModelUsage(12, 24, 36);
        ProviderErrorSummary error = new ProviderErrorSummary(
                new PiError(PiError.Category.MODEL, "provider.rate_limited", PiError.Severity.ERROR,
                        EventVisibility.ADMIN, true, true, false),
                "rate_limited",
                "Provider rate limit exceeded",
                429,
                true,
                true,
                false);
        ToolCall toolCall = new ToolCall("call-1", new RunId("run-1"), new StepId("step-1"),
                "lookup", Map.of("query", "pi"), Instant.parse("2026-06-14T00:00:00Z"));

        ModelStreamChunk.TextDelta text = new ModelStreamChunk.TextDelta(
                "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini",
                1, Duration.ofMillis(42), "hello");
        ModelStreamChunk.ToolCallIntent tool = new ModelStreamChunk.ToolCallIntent(
                "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini",
                2, Duration.ofMillis(50), toolCall);
        ModelStreamChunk.Usage usageChunk = new ModelStreamChunk.Usage(
                "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini",
                3, Duration.ofMillis(55), usage);
        ModelStreamChunk.Finished finished = new ModelStreamChunk.Finished(
                "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini",
                4, Duration.ofMillis(60), ModelFinishReason.STOP, usage);
        ModelStreamChunk.ProviderError errorChunk = new ModelStreamChunk.ProviderError(
                "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini",
                5, Duration.ofMillis(61), error);

        assertThat(text.textDelta()).isEqualTo("hello");
        assertThat(tool.toolCall()).isEqualTo(toolCall);
        assertThat(usageChunk.usage().inputTokens()).isEqualTo(12);
        assertThat(finished.finishReason()).isEqualTo(ModelFinishReason.STOP);
        assertThat(errorChunk.errorSummary().piError().category()).isEqualTo(PiError.Category.MODEL);
        assertThat(errorChunk.errorSummary().safeMessage()).doesNotContain("Authorization", "api_key", "sk-");
    }

    @Test
    void streaming_model_client_uses_pi_owned_sink_and_cancellation_token() {
        List<ModelStreamChunk> chunks = new ArrayList<>();
        StreamingModelClient client = (request, cancellationToken, sink) -> {
            assertThat(cancellationToken.isCancellationRequested()).isFalse();
            sink.accept(new ModelStreamChunk.TextDelta(
                    "fake", "fake-model", "fake:fake-model", 1, Duration.ZERO, "delta"));
        };

        client.stream(null, new CancellationToken(), chunks::add);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst()).isInstanceOf(ModelStreamChunk.TextDelta.class);
    }
}
