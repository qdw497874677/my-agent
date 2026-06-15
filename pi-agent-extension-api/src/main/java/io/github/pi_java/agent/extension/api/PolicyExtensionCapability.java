package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;

public record PolicyExtensionCapability(
        String capabilityId,
        String policyRef,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public PolicyExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        policyRef = ExtensionStrings.requireNonBlank(policyRef, "policyRef");
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.POLICY;
    }
}
