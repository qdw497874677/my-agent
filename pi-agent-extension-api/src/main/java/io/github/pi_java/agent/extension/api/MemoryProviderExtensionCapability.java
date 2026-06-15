package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record MemoryProviderExtensionCapability(
        String capabilityId,
        String providerId,
        Set<String> memoryKinds,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public MemoryProviderExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        providerId = ExtensionStrings.requireNonBlank(providerId, "providerId");
        memoryKinds = Set.copyOf(Objects.requireNonNull(memoryKinds, "memoryKinds must not be null"));
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.MEMORY_PROVIDER;
    }
}
