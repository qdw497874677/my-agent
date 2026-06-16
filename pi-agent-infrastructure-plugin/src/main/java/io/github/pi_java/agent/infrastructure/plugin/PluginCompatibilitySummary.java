package io.github.pi_java.agent.infrastructure.plugin;

import java.util.Objects;

public record PluginCompatibilitySummary(
        String declaredRange,
        String platformApiVersion,
        boolean compatible,
        String status
) {
    public PluginCompatibilitySummary {
        declaredRange = requireNonBlank(declaredRange, "declaredRange");
        platformApiVersion = requireNonBlank(platformApiVersion, "platformApiVersion");
        status = requireNonBlank(status, "status");
    }

    public static PluginCompatibilitySummary compatible(String declaredRange, String platformApiVersion) {
        return new PluginCompatibilitySummary(declaredRange, platformApiVersion, true, "COMPATIBLE");
    }

    public static PluginCompatibilitySummary incompatible(String declaredRange, String platformApiVersion) {
        return new PluginCompatibilitySummary(declaredRange, platformApiVersion, false, "INCOMPATIBLE");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
