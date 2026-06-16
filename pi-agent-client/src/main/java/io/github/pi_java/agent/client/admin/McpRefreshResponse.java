package io.github.pi_java.agent.client.admin;

import java.util.Map;
import java.util.Objects;

public record McpRefreshResponse(
        boolean refreshed,
        int serverCount,
        int refreshedServerCount,
        int failedServerCount,
        String status,
        String redactedError,
        Map<String, String> metadata
) {
    public McpRefreshResponse {
        if (serverCount < 0) {
            throw new IllegalArgumentException("serverCount must not be negative");
        }
        if (refreshedServerCount < 0) {
            throw new IllegalArgumentException("refreshedServerCount must not be negative");
        }
        if (failedServerCount < 0) {
            throw new IllegalArgumentException("failedServerCount must not be negative");
        }
        status = requireNonBlank(status, "status");
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
