package io.github.pi_java.agent.client.admin;

import java.util.List;

public record ExtensionSourceDto(
        String sourceId,
        String name,
        String version,
        String kind,
        String lifecycleStatus,
        boolean enabled,
        String compatibilityStatus,
        String healthStatus,
        String redactedError,
        List<ExtensionCapabilityDto> capabilities
) {
    public ExtensionSourceDto {
        sourceId = requireNonBlank(sourceId, "sourceId");
        name = requireNonBlank(name, "name");
        version = version == null ? "" : version;
        kind = requireNonBlank(kind, "kind");
        lifecycleStatus = requireNonBlank(lifecycleStatus, "lifecycleStatus");
        compatibilityStatus = requireNonBlank(compatibilityStatus, "compatibilityStatus");
        healthStatus = requireNonBlank(healthStatus, "healthStatus");
        redactedError = redactedError == null ? "" : redactedError;
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
