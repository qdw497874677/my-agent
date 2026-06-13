package io.github.pi_java.agent.domain.tool;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ToolCall(
        String toolCallId,
        RunId runId,
        StepId stepId,
        String toolName,
        Map<String, Object> arguments,
        Instant requestedAt
) {

    public ToolCall {
        toolCallId = requireNonBlank(toolCallId, "toolCallId");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        toolName = requireNonBlank(toolName, "toolName");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
