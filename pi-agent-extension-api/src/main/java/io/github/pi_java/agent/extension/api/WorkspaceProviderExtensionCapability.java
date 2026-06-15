package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record WorkspaceProviderExtensionCapability(
        String capabilityId,
        String providerId,
        Set<String> resourceKinds,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public WorkspaceProviderExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        providerId = ExtensionStrings.requireNonBlank(providerId, "providerId");
        resourceKinds = Set.copyOf(Objects.requireNonNull(resourceKinds, "resourceKinds must not be null"));
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.WORKSPACE_PROVIDER;
    }
}
