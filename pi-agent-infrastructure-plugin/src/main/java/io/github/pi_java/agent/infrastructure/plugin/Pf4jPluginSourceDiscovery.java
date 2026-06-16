package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Pf4jPluginSourceDiscovery {

    private final PluginManager pluginManager;
    private final Pf4jPluginExtensionBridge bridge;

    public Pf4jPluginSourceDiscovery(PluginManager pluginManager, Path controlledDirectory, String platformApiVersion) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.bridge = new Pf4jPluginExtensionBridge(controlledDirectory, platformApiVersion);
    }

    public List<PluginDiscoveredSource> discover() {
        List<PluginDiscoveredSource> discovered = new ArrayList<>();
        for (PluginWrapper wrapper : pluginManager.getPlugins()) {
            List<ExtensionSource> extensions = pluginManager.getExtensions(ExtensionSource.class, wrapper.getPluginId());
            discovered.addAll(bridge.bridge(wrapper, extensions));
        }
        return List.copyOf(discovered);
    }

    public record PluginDiscoveredSource(
            String pluginId,
            PluginDescriptorSummary descriptor,
            PluginLifecycleSummary lifecycle,
            ServiceLoaderExtensionDiscovery.DiscoveredSource discoveredSource
    ) {
        public PluginDiscoveredSource {
            pluginId = requireNonBlank(pluginId, "pluginId");
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            Objects.requireNonNull(lifecycle, "lifecycle must not be null");
            Objects.requireNonNull(discoveredSource, "discoveredSource must not be null");
        }

        static PluginDiscoveredSource discovered(PluginDescriptorSummary descriptor,
                                                 PluginLifecycleSummary lifecycle,
                                                 ExtensionSource source) {
            return new PluginDiscoveredSource(descriptor.pluginId(), descriptor, lifecycle,
                    ServiceLoaderExtensionDiscovery.DiscoveredSource.discovered(source));
        }

        static PluginDiscoveredSource failed(PluginDescriptorSummary descriptor,
                                             PluginLifecycleSummary lifecycle,
                                             String redactedError) {
            return new PluginDiscoveredSource(descriptor.pluginId(), descriptor, lifecycle,
                    ServiceLoaderExtensionDiscovery.DiscoveredSource.failed(descriptor.pluginId(), redactedError));
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
