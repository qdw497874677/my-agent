package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.tool.ToolCall;

import java.time.Duration;
import java.util.Objects;

public sealed interface ModelStreamChunk permits ModelStreamChunk.TextDelta,
        ModelStreamChunk.ToolCallIntent, ModelStreamChunk.Usage,
        ModelStreamChunk.Finished, ModelStreamChunk.Cancelled,
        ModelStreamChunk.TimedOut, ModelStreamChunk.ProviderError {

    String providerId();

    String modelId();

    String modelRef();

    long sequence();

    Duration latency();

    record TextDelta(String providerId, String modelId, String modelRef, long sequence,
                     Duration latency, String textDelta) implements ModelStreamChunk {
        public TextDelta {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            textDelta = Objects.requireNonNull(textDelta, "textDelta must not be null");
        }
    }

    record ToolCallIntent(String providerId, String modelId, String modelRef, long sequence,
                          Duration latency, ToolCall toolCall) implements ModelStreamChunk {
        public ToolCallIntent {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            Objects.requireNonNull(toolCall, "toolCall must not be null");
        }
    }

    record Usage(String providerId, String modelId, String modelRef, long sequence,
                 Duration latency, ModelUsage usage) implements ModelStreamChunk {
        public Usage {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            Objects.requireNonNull(usage, "usage must not be null");
        }
    }

    record Finished(String providerId, String modelId, String modelRef, long sequence,
                    Duration latency, ModelFinishReason finishReason, ModelUsage usage) implements ModelStreamChunk {
        public Finished {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            Objects.requireNonNull(finishReason, "finishReason must not be null");
        }
    }

    record Cancelled(String providerId, String modelId, String modelRef, long sequence,
                     Duration latency, String reason) implements ModelStreamChunk {
        public Cancelled {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            reason = requireNonBlank(reason, "reason");
        }
    }

    record TimedOut(String providerId, String modelId, String modelRef, long sequence,
                    Duration latency, Duration timeout) implements ModelStreamChunk {
        public TimedOut {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            Objects.requireNonNull(timeout, "timeout must not be null");
            if (timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
        }
    }

    record ProviderError(String providerId, String modelId, String modelRef, long sequence,
                         Duration latency, ProviderErrorSummary errorSummary) implements ModelStreamChunk {
        public ProviderError {
            requireBase(providerId, modelId, modelRef, sequence, latency);
            Objects.requireNonNull(errorSummary, "errorSummary must not be null");
        }
    }

    private static void requireBase(String providerId, String modelId, String modelRef, long sequence, Duration latency) {
        requireNonBlank(providerId, "providerId");
        requireNonBlank(modelId, "modelId");
        requireNonBlank(modelRef, "modelRef");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        Objects.requireNonNull(latency, "latency must not be null");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
