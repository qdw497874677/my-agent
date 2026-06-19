package io.github.pi_java.agent.client.admin;

import java.util.Map;
import java.util.Objects;

public record OperationalWarningDto(
        String area,
        String severity,
        String message,
        Map<String, String> metadata
) {
    public OperationalWarningDto {
        area = requireNonBlank(area, "area");
        severity = requireNonBlank(severity, "severity");
        message = requireNonBlank(message, "message");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
