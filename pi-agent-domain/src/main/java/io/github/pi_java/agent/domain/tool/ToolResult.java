package io.github.pi_java.agent.domain.tool;

import io.github.pi_java.agent.domain.error.PiError;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ToolResult(
        String toolCallId,
        boolean success,
        String summary,
        PiError error,
        Instant completedAt
) {

    public ToolResult {
        if (toolCallId == null || toolCallId.isBlank()) {
            throw new IllegalArgumentException("toolCallId must not be blank");
        }
        summary = Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public Optional<PiError> optionalError() {
        return Optional.ofNullable(error);
    }
}
