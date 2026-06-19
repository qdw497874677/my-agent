package io.github.pi_java.agent.client.admin;

import java.util.Map;
import java.util.Objects;

public record OperationMetricDto(
        String area,
        String name,
        String status,
        double value,
        String unit,
        Map<String, String> metadata
) {
    public OperationMetricDto {
        area = requireNonBlank(area, "area");
        name = requireNonBlank(name, "name");
        status = requireNonBlank(status, "status");
        unit = requireNonBlank(unit, "unit");
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException("value must be finite and non-negative");
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
