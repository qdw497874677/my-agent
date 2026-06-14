package io.github.pi_java.agent.domain.model;

import java.util.Map;
import java.util.Objects;

public record ModelDescriptor(
        String providerId,
        String modelId,
        String displayName,
        ModelCapabilities capabilities,
        Map<String, String> extraMetadata
) {
    public ModelDescriptor {
        providerId = DomainModelValidation.requireNonBlank(providerId, "providerId");
        modelId = DomainModelValidation.requireNonBlank(modelId, "modelId");
        displayName = DomainModelValidation.requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        extraMetadata = Map.copyOf(Objects.requireNonNull(extraMetadata, "extraMetadata must not be null"));
    }
}
