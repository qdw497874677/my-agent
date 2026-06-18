package io.github.pi_java.agent.infrastructure.plugin;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class Pf4jControlledPluginDiscoveryService {

    private final PluginRegistryProperties properties;

    public Pf4jControlledPluginDiscoveryService(PluginRegistryProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discover() {
        if (!properties.enabled() || properties.pluginDirectory().isEmpty() || !properties.loadOnStartup()) {
            return List.of();
        }
        Path directory = properties.pluginDirectory().orElseThrow();
        PluginManager pluginManager = new DefaultPluginManager(List.of(directory));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        return new Pf4jPluginSourceDiscovery(pluginManager, directory, properties.platformApiVersion()).discover();
    }
}
