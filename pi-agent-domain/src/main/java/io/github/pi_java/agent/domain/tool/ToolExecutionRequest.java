package io.github.pi_java.agent.domain.tool;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ToolExecutionRequest(
        String toolCallId,
        RunId runId,
        StepId stepId,
        String toolId,
        String descriptorVersion,
        Map<String, Object> arguments,
        Instant requestedAt
) {
    public ToolExecutionRequest {
        toolCallId = ToolValidation.requireNonBlank(toolCallId, "toolCallId");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        toolId = ToolValidation.requireNonBlank(toolId, "toolId");
        descriptorVersion = ToolValidation.requireNonBlank(descriptorVersion, "descriptorVersion");
        arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
