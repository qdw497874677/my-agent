package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExtensionToolRegistry implements ToolRegistry {

    private final DefaultExtensionContributionRegistry contributions;

    public ExtensionToolRegistry(DefaultExtensionContributionRegistry contributions) {
        this.contributions = java.util.Objects.requireNonNull(contributions, "contributions must not be null");
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return contributions.usableCapabilities().stream()
                .filter(entry -> entry.capability() instanceof ToolExtensionCapability)
                .map(entry -> normalize(entry, ((ToolExtensionCapability) entry.capability()).descriptor()))
                .toList();
    }

    @Override
    public Optional<ToolResolution> resolve(String toolId) {
        return contributions.usableCapabilities().stream()
                .filter(entry -> entry.capability() instanceof ToolExtensionCapability)
                .map(entry -> new ResolvedEntry(entry, (ToolExtensionCapability) entry.capability()))
                .filter(entry -> normalize(entry.entry(), entry.capability().descriptor()).id().equals(toolId))
                .findFirst()
                .map(entry -> new ToolResolution(normalize(entry.entry(), entry.capability().descriptor()),
                        entry.capability().binding()));
    }

    private ToolDescriptor normalize(DefaultExtensionContributionRegistry.CapabilityEntry entry, ToolDescriptor descriptor) {
        ToolProvenance provenance = descriptor.provenance();
        Map<String, String> metadata = new LinkedHashMap<>(provenance.metadata());
        metadata.put("extension.sourceId", entry.sourceId());
        metadata.put("extension.capabilityId", entry.capabilityId());
        ToolProvenance normalizedProvenance = new ToolProvenance(
                ToolProvenance.SourceKind.SPI,
                entry.sourceId(),
                provenance.bindingRef(),
                metadata);
        return new ToolDescriptor(descriptor.id(), descriptor.name(), descriptor.description(), descriptor.inputSchema(),
                descriptor.outputSchema(), normalizedProvenance, descriptor.version(), descriptor.scopes(),
                descriptor.riskLevel(), descriptor.sideEffect(), descriptor.defaultTimeout(), descriptor.metadata());
    }

    private record ResolvedEntry(DefaultExtensionContributionRegistry.CapabilityEntry entry,
                                 ToolExtensionCapability capability) {
    }
}
