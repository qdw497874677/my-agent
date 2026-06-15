package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record PolicyDecisionSummaryDto(
        String id,
        String decision,
        String reason,
        String toolId,
        String toolCallId,
        String sessionId,
        String runId,
        Instant decidedAt,
        Map<String, String> redactedSummary
) {
    public PolicyDecisionSummaryDto {
        id = requireNonBlank(id, "id");
        decision = requireNonBlank(decision, "decision");
        reason = reason == null ? "" : reason;
        toolId = toolId == null ? "" : toolId;
        toolCallId = toolCallId == null ? "" : toolCallId;
        sessionId = sessionId == null ? "" : sessionId;
        runId = runId == null ? "" : runId;
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        redactedSummary = Map.copyOf(Objects.requireNonNull(redactedSummary, "redactedSummary must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
