package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.tool.ToolCall;

import java.util.Objects;

public sealed interface ModelResponse permits ModelResponse.FinalText, ModelResponse.ToolCallIntent {
    record FinalText(String text) implements ModelResponse {
        public FinalText {
            text = Objects.requireNonNull(text, "text must not be null");
        }
    }

    record ToolCallIntent(ToolCall toolCall) implements ModelResponse {
        public ToolCallIntent {
            Objects.requireNonNull(toolCall, "toolCall must not be null");
        }
    }
}
