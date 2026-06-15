package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultExtensionContributionRegistry {

    private final List<SourceEntry> sourceEntries;
    private final List<CapabilityEntry> capabilityEntries;

    DefaultExtensionContributionRegistry(List<SourceEntry> sourceEntries, List<CapabilityEntry> capabilityEntries) {
        this.sourceEntries = List.copyOf(sourceEntries);
        this.capabilityEntries = List.copyOf(capabilityEntries);
    }

    public static DefaultExtensionContributionRegistry build(
            List<ServiceLoaderExtensionDiscovery.DiscoveredSource> discoveredSources,
            ExtensionRegistrationProperties properties) {
        Objects.requireNonNull(discoveredSources, "discoveredSources must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        ExtensionCompatibilityChecker checker = new ExtensionCompatibilityChecker(
                ExtensionApiVersion.parse(properties.platformApiVersion()));
        List<SourceEntry> sources = new ArrayList<>();
        List<CapabilityEntry> capabilities = new ArrayList<>();

        for (ServiceLoaderExtensionDiscovery.DiscoveredSource discovered : discoveredSources) {
            if (discovered.status() == ServiceLoaderExtensionDiscovery.DiscoveryStatus.FAILED) {
                sources.add(SourceEntry.failed(discovered.sourceId(), discovered.redactedError()));
                continue;
            }
            ExtensionSource source = discovered.source().orElseThrow();
            ExtensionMetadata metadata = source.metadata();
            boolean sourceDisabled = properties.disabledSources().contains(metadata.extensionId()) || !metadata.enabled();
            boolean compatible = checker.supports(metadata.compatibility());
            boolean available = metadata.lifecycleState().isAvailable()
                    && metadata.health().status() != io.github.pi_java.agent.extension.api.ExtensionHealth.Status.DOWN;
            SourceEntry sourceEntry = SourceEntry.from(source, metadata, sourceDisabled, compatible, available, "");
            sources.add(sourceEntry);
            for (ExtensionCapability capability : source.capabilities()) {
                boolean capabilityDisabled = properties.disabledCapabilities().contains(capability.capabilityId());
                capabilities.add(CapabilityEntry.from(metadata.extensionId(), capability, sourceEntry, capabilityDisabled));
            }
        }

        sources.sort(Comparator.comparingInt(SourceEntry::order).thenComparing(SourceEntry::sourceId));
        capabilities.sort(Comparator.comparingInt(CapabilityEntry::sourceOrder)
                .thenComparing(CapabilityEntry::sourceId)
                .thenComparingInt(CapabilityEntry::order)
                .thenComparing(CapabilityEntry::capabilityId));
        capabilities = mergeDuplicates(capabilities, properties.allowDuplicateCapabilityOverrides());
        return new DefaultExtensionContributionRegistry(sources, capabilities);
    }

    public List<SourceEntry> sourceEntries() {
        return sourceEntries;
    }

    public List<CapabilityEntry> capabilityEntries() {
        return capabilityEntries;
    }

    public List<CapabilityEntry> usableCapabilities() {
        return capabilityEntries.stream().filter(CapabilityEntry::usable).toList();
    }

    public Optional<CapabilityEntry> usableCapability(String capabilityId) {
        return usableCapabilities().stream().filter(entry -> entry.capabilityId().equals(capabilityId)).findFirst();
    }

    private static List<CapabilityEntry> mergeDuplicates(List<CapabilityEntry> capabilities, boolean allowOverrides) {
        Map<String, CapabilityEntry> byId = new LinkedHashMap<>();
        for (CapabilityEntry entry : capabilities) {
            CapabilityEntry previous = byId.put(entry.capabilityId(), entry);
            if (previous != null && !allowOverrides) {
                throw new IllegalStateException("Duplicate extension capability id '%s' from sources '%s' and '%s'"
                        .formatted(entry.capabilityId(), previous.sourceId(), entry.sourceId()));
            }
        }
        return List.copyOf(byId.values());
    }

    static int order(Map<String, Object> metadata) {
        Object value = metadata.get("order");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public record SourceEntry(
            String sourceId,
            String name,
            String version,
            String status,
            boolean enabled,
            String compatibilityStatus,
            String healthStatus,
            String redactedError,
            int order,
            ExtensionSource source
    ) {
        static SourceEntry from(ExtensionSource source, ExtensionMetadata metadata, boolean disabled, boolean compatible,
                                boolean available, String redactedError) {
            String status = disabled ? "DISABLED" : compatible && available ? "USABLE" : "UNUSABLE";
            return new SourceEntry(metadata.extensionId(), metadata.name(), metadata.version(), status, !disabled,
                    compatible ? "COMPATIBLE" : "INCOMPATIBLE", metadata.health().status().name(), redactedError,
                    DefaultExtensionContributionRegistry.order(metadata.redactedMetadata()), source);
        }

        static SourceEntry failed(String sourceId, String redactedError) {
            return new SourceEntry(sourceId, sourceId, "", "FAILED", false, "UNKNOWN", "DOWN", redactedError,
                    Integer.MAX_VALUE, null);
        }

        public boolean usable() {
            return enabled && "COMPATIBLE".equals(compatibilityStatus) && "USABLE".equals(status);
        }
    }

    public record CapabilityEntry(
            String sourceId,
            ExtensionCapability capability,
            String capabilityId,
            String type,
            String status,
            boolean enabled,
            String compatibilityStatus,
            String healthStatus,
            int sourceOrder,
            int order
    ) {
        static CapabilityEntry from(String sourceId, ExtensionCapability capability, SourceEntry sourceEntry,
                                    boolean capabilityDisabled) {
            boolean enabled = sourceEntry.enabled() && !capabilityDisabled;
            String status = enabled && sourceEntry.usable() ? "USABLE" : capabilityDisabled ? "DISABLED" : sourceEntry.status();
            return new CapabilityEntry(sourceId, capability, capability.capabilityId(), capability.type().name(), status,
                    enabled, sourceEntry.compatibilityStatus(), sourceEntry.healthStatus(), sourceEntry.order(),
                    DefaultExtensionContributionRegistry.order(capability.redactedMetadata()));
        }

        public boolean usable() {
            return enabled && "COMPATIBLE".equals(compatibilityStatus) && "USABLE".equals(status);
        }
    }
}
