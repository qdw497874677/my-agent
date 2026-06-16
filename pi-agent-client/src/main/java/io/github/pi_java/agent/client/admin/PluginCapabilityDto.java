package io.github.pi_java.agent.client.admin;

import java.util.Map;
import java.util.Objects;

public record PluginCapabilityDto(
        String capabilityId,
        String type,
        String status,
        String version,
        String pluginId,
        boolean enabled,
        String compatibilityStatus,
        String healthStatus,
        Map<String, String> metadata
) {
    public PluginCapabilityDto {
        capabilityId = requireNonBlank(capabilityId, "capabilityId");
        type = requireNonBlank(type, "type");
        status = requireNonBlank(status, "status");
        version = version == null ? "" : version;
        pluginId = requireNonBlank(pluginId, "pluginId");
        compatibilityStatus = requireNonBlank(compatibilityStatus, "compatibilityStatus");
        healthStatus = requireNonBlank(healthStatus, "healthStatus");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
