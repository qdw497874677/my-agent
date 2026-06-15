package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditSummaryDto(
        String id,
        String action,
        String resourceType,
        String resourceId,
        String sessionId,
        String runId,
        Instant recordedAt,
        Map<String, String> redactedDetails
) {
    public AuditSummaryDto {
        id = requireNonBlank(id, "id");
        action = requireNonBlank(action, "action");
        resourceType = resourceType == null ? "" : resourceType;
        resourceId = resourceId == null ? "" : resourceId;
        sessionId = sessionId == null ? "" : sessionId;
        runId = runId == null ? "" : runId;
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        redactedDetails = Map.copyOf(Objects.requireNonNull(redactedDetails, "redactedDetails must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
