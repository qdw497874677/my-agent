package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.app.port.extension.ExtensionCapabilityStatus;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ExtensionGovernanceCatalogAdapter implements ExtensionGovernanceCatalog {

    private final DefaultExtensionContributionRegistry contributions;

    public ExtensionGovernanceCatalogAdapter(DefaultExtensionContributionRegistry contributions) {
        this.contributions = java.util.Objects.requireNonNull(contributions, "contributions must not be null");
    }

    @Override
    public List<ExtensionSourceStatus> sources() {
        Map<String, List<ExtensionCapabilityStatus>> capabilitiesBySource = contributions.capabilityEntries().stream()
                .map(this::toCapabilityStatus)
                .collect(Collectors.groupingBy(ExtensionCapabilityStatus::sourceId));
        return contributions.sourceEntries().stream()
                .map(source -> new ExtensionSourceStatus(source.sourceId(), source.name(), source.version(), sourceKind(source),
                        source.status(), source.enabled(), source.compatibilityStatus(), source.healthStatus(),
                        source.redactedError(), capabilitiesBySource.getOrDefault(source.sourceId(), List.of())))
                .toList();
    }

    private ExtensionCapabilityStatus toCapabilityStatus(DefaultExtensionContributionRegistry.CapabilityEntry entry) {
        return new ExtensionCapabilityStatus(entry.capabilityId(), entry.type(), entry.status(),
                metadataString(entry.capability().redactedMetadata(), "version"), entry.sourceId(), entry.enabled(),
                entry.compatibilityStatus(), entry.healthStatus(), stringMetadata(entry));
    }

    private static Map<String, String> stringMetadata(DefaultExtensionContributionRegistry.CapabilityEntry entry) {
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        entry.capability().redactedMetadata().forEach((key, value) -> metadata.put(key, String.valueOf(value)));
        metadata.put("extension.sourceId", entry.sourceId());
        metadata.put("extension.capabilityId", entry.capabilityId());
        metadata.put("extension.sourceKind", sourceKind(entry));
        return Map.copyOf(metadata);
    }

    private static String sourceKind(DefaultExtensionContributionRegistry.SourceEntry entry) {
        if (entry.source() != null) {
            Object value = entry.source().metadata().redactedMetadata().get("sourceKind");
            if (value instanceof String string && !string.isBlank()) {
                return string;
            }
        }
        return "SPI";
    }

    private static String sourceKind(DefaultExtensionContributionRegistry.CapabilityEntry entry) {
        Object value = entry.capability().redactedMetadata().get("sourceKind");
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        return "SPI";
    }

    private static String metadataString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
