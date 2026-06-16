package io.github.pi_java.agent.app.port.mcp;

import java.util.Map;
import java.util.Objects;

public record McpToolStatus(
        String serverQualifiedToolId,
        String mcpToolName,
        String availabilityStatus,
        boolean readOnly,
        boolean destructive,
        boolean openWorld,
        String schemaSummary,
        String redactedError,
        Map<String, String> metadata
) {
    public McpToolStatus {
        serverQualifiedToolId = requireNonBlank(serverQualifiedToolId, "serverQualifiedToolId");
        mcpToolName = requireNonBlank(mcpToolName, "mcpToolName");
        availabilityStatus = requireNonBlank(availabilityStatus, "availabilityStatus");
        schemaSummary = schemaSummary == null ? "" : schemaSummary;
        redactedError = redactedError == null ? "" : redactedError;
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
