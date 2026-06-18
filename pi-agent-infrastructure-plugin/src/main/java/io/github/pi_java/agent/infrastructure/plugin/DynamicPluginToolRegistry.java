package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class DynamicPluginToolRegistry implements ToolRegistry {

    private final Supplier<DefaultExtensionContributionRegistry> contributionSupplier;

    public DynamicPluginToolRegistry(Supplier<DefaultExtensionContributionRegistry> contributionSupplier) {
        this.contributionSupplier = Objects.requireNonNull(contributionSupplier, "contributionSupplier must not be null");
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return new ExtensionToolRegistry(contributionSupplier.get()).listTools();
    }

    @Override
    public Optional<ToolResolution> resolve(String toolId) {
        return new ExtensionToolRegistry(contributionSupplier.get()).resolve(toolId);
    }
}
