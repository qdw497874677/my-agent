package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PluginSourceDto(
        String pluginId,
        String name,
        String version,
        String vendor,
        String sourceKind,
        String lifecycleStatus,
        boolean enabled,
        String healthStatus,
        String compatibilityStatus,
        int capabilityCount,
        Map<String, String> capabilityStatusCounts,
        String redactedError,
        String relativePathSummary,
        String reason,
        Instant lastUpdatedAt,
        List<PluginCapabilityDto> capabilities,
        Map<String, String> metadata
) {
    public PluginSourceDto {
        pluginId = requireNonBlank(pluginId, "pluginId");
        name = requireNonBlank(name, "name");
        version = version == null ? "" : version;
        vendor = vendor == null ? "" : vendor;
        sourceKind = requireNonBlank(sourceKind, "sourceKind");
        lifecycleStatus = requireNonBlank(lifecycleStatus, "lifecycleStatus");
        healthStatus = requireNonBlank(healthStatus, "healthStatus");
        compatibilityStatus = requireNonBlank(compatibilityStatus, "compatibilityStatus");
        if (capabilityCount < 0) {
            throw new IllegalArgumentException("capabilityCount must not be negative");
        }
        capabilityStatusCounts = Map.copyOf(Objects.requireNonNull(
                capabilityStatusCounts, "capabilityStatusCounts must not be null"));
        redactedError = redactedError == null ? "" : redactedError;
        relativePathSummary = relativePathSummary == null ? "" : relativePathSummary;
        reason = reason == null ? "" : reason;
        lastUpdatedAt = Objects.requireNonNull(lastUpdatedAt, "lastUpdatedAt must not be null");
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
