package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;

import java.time.Instant;
import java.util.Optional;

public interface PluginStateStore {

    Optional<PluginStateRecord> state(String pluginId);

    PluginStateRecord disable(String pluginId, String actor, String reason);

    PluginStateRecord quarantine(String pluginId, String actor, String reason);

    record PluginStateRecord(
            String pluginId,
            ExtensionLifecycleState lifecycleState,
            String actor,
            String reason,
            Instant updatedAt
    ) {
    }
}
