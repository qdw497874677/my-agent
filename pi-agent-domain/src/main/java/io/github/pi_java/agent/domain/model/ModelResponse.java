package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.tool.ToolCall;

import java.time.Duration;
import java.util.Objects;

public sealed interface ModelResponse permits ModelResponse.FinalText, ModelResponse.ToolCallIntent {
    record FinalText(String text, String providerId, String modelId, String modelRef,
                     ModelFinishReason finishReason, ModelUsage usage, Duration latency) implements ModelResponse {
        public FinalText(String text) {
            this(text, null, null, null, null, null, null);
        }

        public FinalText {
            text = Objects.requireNonNull(text, "text must not be null");
            validateMetadata(providerId, modelId, modelRef, latency);
        }
    }

    record ToolCallIntent(ToolCall toolCall, String providerId, String modelId, String modelRef,
                          ModelFinishReason finishReason, ModelUsage usage, Duration latency) implements ModelResponse {
        public ToolCallIntent(ToolCall toolCall) {
            this(toolCall, null, null, null, null, null, null);
        }

        public ToolCallIntent {
            Objects.requireNonNull(toolCall, "toolCall must not be null");
            validateMetadata(providerId, modelId, modelRef, latency);
        }
    }

    private static void validateMetadata(String providerId, String modelId, String modelRef, Duration latency) {
        requireNonBlankIfPresent(providerId, "providerId");
        requireNonBlankIfPresent(modelId, "modelId");
        requireNonBlankIfPresent(modelRef, "modelRef");
        if (latency != null && latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
    }

    private static void requireNonBlankIfPresent(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when present");
        }
    }
}
