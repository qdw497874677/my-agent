package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ModelProviderExtensionCapability(
        String capabilityId,
        String providerId,
        Set<String> advertisedCapabilities,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public ModelProviderExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        providerId = ExtensionStrings.requireNonBlank(providerId, "providerId");
        advertisedCapabilities = Set.copyOf(Objects.requireNonNull(advertisedCapabilities,
                "advertisedCapabilities must not be null"));
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.MODEL_PROVIDER;
    }
}
