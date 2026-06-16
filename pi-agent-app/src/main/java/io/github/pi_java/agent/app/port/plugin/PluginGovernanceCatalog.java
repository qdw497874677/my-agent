package io.github.pi_java.agent.app.port.plugin;

import java.util.List;

public interface PluginGovernanceCatalog {

    List<PluginSourceStatus> plugins();

    PluginMutationStatus refresh();

    PluginMutationStatus disable(String pluginId, String actor, String reason);

    PluginMutationStatus quarantine(String pluginId, String actor, String reason);

    default String overallStatus() {
        List<PluginSourceStatus> statuses = plugins();
        if (statuses.isEmpty()) {
            return "UNCONFIGURED";
        }
        boolean anyQuarantined = statuses.stream()
                .anyMatch(plugin -> "QUARANTINED".equals(plugin.lifecycleStatus()));
        if (anyQuarantined) {
            return "QUARANTINED";
        }
        boolean anyFailed = statuses.stream()
                .anyMatch(plugin -> "FAILED".equals(plugin.lifecycleStatus())
                        || "UNHEALTHY".equals(plugin.healthStatus())
                        || "INCOMPATIBLE".equals(plugin.compatibilityStatus()));
        if (anyFailed) {
            return "DEGRADED";
        }
        boolean anyDisabled = statuses.stream().anyMatch(plugin -> !plugin.enabled());
        return anyDisabled ? "PARTIAL" : "HEALTHY";
    }
}
