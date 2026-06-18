package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class PluginGovernanceCatalogAdapter {

    private final AtomicReference<List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource>> discoveredPlugins;
    private final PluginStateStore stateStore;
    private final ExtensionRegistrationProperties properties;
    private final PluginRegistryProperties registryProperties;
    private final Supplier<List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource>> discoverySupplier;

    public PluginGovernanceCatalogAdapter(List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discoveredPlugins,
                                          PluginStateStore stateStore,
                                          ExtensionRegistrationProperties properties) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.discoveredPlugins = new AtomicReference<>(List.copyOf(Objects.requireNonNull(discoveredPlugins,
                "discoveredPlugins must not be null")));
        this.registryProperties = null;
        this.discoverySupplier = null;
    }

    public PluginGovernanceCatalogAdapter(List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discoveredPlugins,
                                          PluginStateStore stateStore,
                                          ExtensionRegistrationProperties properties,
                                          PluginRegistryProperties registryProperties,
                                          Supplier<List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource>> discoverySupplier) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.registryProperties = Objects.requireNonNull(registryProperties, "registryProperties must not be null");
        this.discoverySupplier = Objects.requireNonNull(discoverySupplier, "discoverySupplier must not be null");
        this.discoveredPlugins = new AtomicReference<>(List.copyOf(Objects.requireNonNull(discoveredPlugins,
                "discoveredPlugins must not be null")));
    }

    public DefaultExtensionContributionRegistry contributionRegistry() {
        return DefaultExtensionContributionRegistry.build(toDiscoveredSources(), effectiveProperties());
    }

    public List<PluginSourceStatus> plugins() {
        DefaultExtensionContributionRegistry contributionRegistry = contributionRegistry();
        Map<String, List<DefaultExtensionContributionRegistry.CapabilityEntry>> capabilitiesBySource = contributionRegistry
                .capabilityEntries().stream().collect(Collectors.groupingBy(DefaultExtensionContributionRegistry.CapabilityEntry::sourceId));
        List<PluginSourceStatus> statuses = new ArrayList<>();
        for (Pf4jPluginSourceDiscovery.PluginDiscoveredSource plugin : discoveredPlugins.get()) {
            String sourceId = plugin.discoveredSource().sourceId();
            List<PluginCapabilityStatus> capabilities = capabilitiesBySource.getOrDefault(sourceId, List.of()).stream()
                    .map(entry -> capabilityStatus(plugin.pluginId(), plugin, entry))
                    .toList();
            statuses.add(sourceStatus(plugin, capabilities));
        }
        return List.copyOf(statuses);
    }

    public PluginMutationStatus refresh() {
        if (registryProperties != null && !registryProperties.manualRefreshEnabled()) {
            return new PluginMutationStatus(false, "", "refresh", "", "", "REFRESH_DISABLED", "",
                    Map.of("mode", "manual"));
        }
        if (discoverySupplier == null) {
            return new PluginMutationStatus(true, "", "refresh", "", "", "REFRESH_REQUESTED", "",
                    Map.of("mode", "manual"));
        }
        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = List.copyOf(discoverySupplier.get());
        discoveredPlugins.set(discovered);
        return new PluginMutationStatus(true, "", "refresh", "", "", "REFRESHED", "",
                Map.of("mode", "manual", "pluginCount", String.valueOf(discovered.size())));
    }

    public PluginMutationStatus disable(String pluginId, String actor, String reason) {
        String previous = stateStore.state(pluginId).map(record -> record.lifecycleState().name()).orElse("");
        PluginStateStore.PluginStateRecord record = stateStore.disable(pluginId, actor, reason);
        return mutation("disable", previous, record, false);
    }

    public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
        String previous = stateStore.state(pluginId).map(record -> record.lifecycleState().name()).orElse("");
        PluginStateStore.PluginStateRecord record = stateStore.quarantine(pluginId, actor, reason);
        return mutation("quarantine", previous, record, true);
    }

    private PluginMutationStatus mutation(String operation, String previous, PluginStateStore.PluginStateRecord record,
                                          boolean operatorActionRequired) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("actor", record.actor());
        metadata.put("reason", record.reason());
        metadata.put("operatorActionRequired", String.valueOf(operatorActionRequired));
        return new PluginMutationStatus(true, record.pluginId(), operation, previous, record.lifecycleState().name(),
                record.lifecycleState().name(), "", metadata);
    }

    private PluginSourceStatus sourceStatus(Pf4jPluginSourceDiscovery.PluginDiscoveredSource plugin,
                                            List<PluginCapabilityStatus> capabilities) {
        PluginStateStore.PluginStateRecord override = stateStore.state(plugin.pluginId()).orElse(null);
        String lifecycle = override == null ? plugin.lifecycle().lifecycleState().name() : override.lifecycleState().name();
        boolean enabled = override == null && plugin.discoveredSource().status() == ServiceLoaderExtensionDiscovery.DiscoveryStatus.DISCOVERED
                && plugin.lifecycle().lifecycleState() != ExtensionLifecycleState.FAILED
                && plugin.descriptor().compatibility().compatible();
        Map<String, String> counts = capabilities.stream()
                .collect(Collectors.groupingBy(PluginCapabilityStatus::status, Collectors.collectingAndThen(Collectors.counting(), String::valueOf)));
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(plugin.descriptor().redactedMetadata());
        metadata.put("operatorActionRequired", String.valueOf(override != null && override.lifecycleState() == ExtensionLifecycleState.QUARANTINED));
        metadata.put("nonSandboxWarning", String.valueOf(plugin.lifecycle().nonSandboxWarning()));
        return new PluginSourceStatus(plugin.pluginId(), plugin.descriptor().name(), plugin.descriptor().version(),
                plugin.descriptor().provider(), "PF4J_JAR", lifecycle, enabled,
                plugin.lifecycle().lifecycleState() == ExtensionLifecycleState.FAILED ? "DOWN" : "UP",
                plugin.descriptor().compatibility().status(), capabilities.size(), counts,
                plugin.lifecycle().redactedError(), plugin.descriptor().sourcePathSummary(),
                override == null ? "" : override.reason(), override == null ? Instant.EPOCH : override.updatedAt(),
                capabilities, metadata);
    }

    private PluginCapabilityStatus capabilityStatus(String pluginId,
                                                    Pf4jPluginSourceDiscovery.PluginDiscoveredSource plugin,
                                                    DefaultExtensionContributionRegistry.CapabilityEntry entry) {
        PluginStateStore.PluginStateRecord override = stateStore.state(plugin.pluginId()).orElse(null);
        boolean forcedOff = override != null && (override.lifecycleState() == ExtensionLifecycleState.DISABLED
                || override.lifecycleState() == ExtensionLifecycleState.QUARANTINED);
        String status = forcedOff ? override.lifecycleState().name() : entry.status();
        boolean enabled = !forcedOff && entry.enabled();
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        entry.capability().redactedMetadata().forEach((key, value) -> metadata.put(key, String.valueOf(value)));
        metadata.put("plugin.id", pluginId);
        return new PluginCapabilityStatus(entry.capabilityId(), entry.type(), status,
                metadata.getOrDefault("version", ""), pluginId, enabled, entry.compatibilityStatus(),
                entry.healthStatus(), metadata);
    }

    private ExtensionRegistrationProperties effectiveProperties() {
        Set<String> disabledSources = discoveredPlugins.get().stream()
                .filter(plugin -> stateStore.state(plugin.pluginId())
                        .map(record -> record.lifecycleState() == ExtensionLifecycleState.DISABLED
                                || record.lifecycleState() == ExtensionLifecycleState.QUARANTINED)
                        .orElse(false))
                .map(plugin -> plugin.discoveredSource().sourceId())
                .collect(Collectors.toSet());
        disabledSources.addAll(properties.disabledSources());
        return new ExtensionRegistrationProperties(disabledSources, properties.disabledCapabilities(),
                properties.allowDuplicateCapabilityOverrides(), properties.platformApiVersion());
    }

    private List<ServiceLoaderExtensionDiscovery.DiscoveredSource> toDiscoveredSources() {
        return discoveredPlugins.get().stream()
                .map(Pf4jPluginSourceDiscovery.PluginDiscoveredSource::discoveredSource)
                .toList();
    }
}
