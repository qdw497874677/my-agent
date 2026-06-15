package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;
import io.github.pi_java.agent.extension.api.ModelProviderExtensionCapability;

import java.util.List;
import java.util.Map;

public final class ExtensionModelProviderRegistry implements ModelProviderRegistry {

    private final DefaultExtensionContributionRegistry contributions;

    public ExtensionModelProviderRegistry(DefaultExtensionContributionRegistry contributions) {
        this.contributions = java.util.Objects.requireNonNull(contributions, "contributions must not be null");
    }

    @Override
    public List<ProviderDescriptor> listProviders() {
        return contributions.usableCapabilities().stream()
                .filter(entry -> entry.capability() instanceof ModelProviderExtensionCapability)
                .map(entry -> toProvider(entry, (ModelProviderExtensionCapability) entry.capability()))
                .toList();
    }

    private ProviderDescriptor toProvider(DefaultExtensionContributionRegistry.CapabilityEntry entry,
                                          ModelProviderExtensionCapability capability) {
        ModelCapabilities capabilities = new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.UNKNOWN,
                intMetadata(capability.redactedMetadata(), "contextWindowTokens", 8192),
                intMetadata(capability.redactedMetadata(), "maxOutputTokens", 2048), true);
        ModelDescriptor defaultModel = new ModelDescriptor(capability.providerId(),
                stringMetadata(capability.redactedMetadata(), "defaultModel", "default"),
                stringMetadata(capability.redactedMetadata(), "displayName", capability.providerId()), capabilities,
                Map.of("extension.sourceId", entry.sourceId(), "extension.capabilityId", entry.capabilityId()));
        return new ProviderDescriptor(capability.providerId(),
                stringMetadata(capability.redactedMetadata(), "displayName", capability.providerId()),
                stringMetadata(capability.redactedMetadata(), "description", "Extension model provider"), null,
                capabilities, List.of(defaultModel),
                Map.of("extension.sourceId", entry.sourceId(),
                        "extension.capabilityId", entry.capabilityId(),
                        "extension.sourceKind", "SPI"));
    }

    private static String stringMetadata(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata.get(key);
        return value instanceof String string && !string.isBlank() ? string : fallback;
    }

    private static int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (value instanceof String string) {
            try {
                return Math.max(1, Integer.parseInt(string));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
