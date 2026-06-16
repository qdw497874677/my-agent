package io.github.pi_java.agent.app.port.plugin;

import java.util.List;
import java.util.Map;

public final class EmptyPluginGovernanceCatalog implements PluginGovernanceCatalog {

    @Override
    public List<PluginSourceStatus> plugins() {
        return List.of();
    }

    @Override
    public PluginMutationStatus refresh() {
        return new PluginMutationStatus(false, "", "refresh", "", "", "UNCONFIGURED", "",
                Map.of("surface", "plugin-governance", "mutation", "refresh"));
    }

    @Override
    public PluginMutationStatus disable(String pluginId, String actor, String reason) {
        return unavailable(pluginId, "disable");
    }

    @Override
    public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
        return unavailable(pluginId, "quarantine");
    }

    private static PluginMutationStatus unavailable(String pluginId, String operation) {
        return new PluginMutationStatus(false, pluginId, operation, "", "", "UNCONFIGURED", "",
                Map.of("surface", "plugin-governance", "mutation", operation));
    }
}
