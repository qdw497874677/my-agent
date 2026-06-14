package io.github.pi_java.agent.domain.tool;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ToolExecutionResult(
        String toolCallId,
        String toolId,
        ToolExecutionStatus status,
        String summary,
        Optional<String> errorCategory,
        Map<String, Object> redactedInputSummary,
        Map<String, Object> redactedOutputSummary,
        Set<String> redactedFields,
        Optional<ProvisionPreview> preview,
        Duration latency,
        Optional<Map<String, Object>> rawOutput
) {
    public ToolExecutionResult(
            String toolCallId,
            String toolId,
            ToolExecutionStatus status,
            String summary,
            String errorCategory,
            Map<String, Object> redactedInputSummary,
            Map<String, Object> redactedOutputSummary,
            Set<String> redactedFields,
            ProvisionPreview preview,
            Duration latency
    ) {
        this(toolCallId, toolId, status, summary, Optional.ofNullable(errorCategory), redactedInputSummary,
                redactedOutputSummary, redactedFields, Optional.ofNullable(preview), latency, Optional.empty());
    }

    public ToolExecutionResult {
        toolCallId = ToolValidation.requireNonBlank(toolCallId, "toolCallId");
        toolId = ToolValidation.requireNonBlank(toolId, "toolId");
        Objects.requireNonNull(status, "status must not be null");
        summary = Objects.requireNonNull(summary, "summary must not be null");
        errorCategory = Objects.requireNonNull(errorCategory, "errorCategory must not be null");
        errorCategory.ifPresent(value -> ToolValidation.requireNonBlank(value, "errorCategory"));
        redactedInputSummary = Map.copyOf(Objects.requireNonNull(redactedInputSummary, "redactedInputSummary must not be null"));
        redactedOutputSummary = Map.copyOf(Objects.requireNonNull(redactedOutputSummary, "redactedOutputSummary must not be null"));
        redactedFields = Set.copyOf(Objects.requireNonNull(redactedFields, "redactedFields must not be null"));
        preview = Objects.requireNonNull(preview, "preview must not be null");
        preview.ifPresent(value -> Objects.requireNonNull(value, "preview must not contain null"));
        Objects.requireNonNull(latency, "latency must not be null");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
        rawOutput = Objects.requireNonNull(rawOutput, "rawOutput must not be null");
        rawOutput = rawOutput.map(Map::copyOf);
    }
}
