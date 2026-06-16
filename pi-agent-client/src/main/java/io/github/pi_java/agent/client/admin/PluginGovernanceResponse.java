package io.github.pi_java.agent.client.admin;

import java.util.List;
import java.util.Objects;

public record PluginGovernanceResponse(List<PluginSourceDto> plugins) {
    public PluginGovernanceResponse {
        plugins = List.copyOf(Objects.requireNonNull(plugins, "plugins must not be null"));
    }
}
