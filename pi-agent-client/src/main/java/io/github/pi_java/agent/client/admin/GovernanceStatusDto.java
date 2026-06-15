package io.github.pi_java.agent.client.admin;

import java.util.Map;
import java.util.Objects;

public record GovernanceStatusDto(
        String area,
        String status,
        String message,
        int count,
        Map<String, String> metadata
) {
    public GovernanceStatusDto {
        area = requireNonBlank(area, "area");
        status = requireNonBlank(status, "status");
        message = message == null ? "" : message;
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
