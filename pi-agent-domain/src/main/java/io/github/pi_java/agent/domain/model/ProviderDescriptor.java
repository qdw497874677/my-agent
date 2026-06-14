package io.github.pi_java.agent.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProviderDescriptor(
        String providerId,
        String displayName,
        String description,
        CredentialRef credentialRef,
        ModelCapabilities defaultCapabilities,
        List<ModelDescriptor> models,
        Map<String, String> extraMetadata
) {
    public ProviderDescriptor {
        providerId = DomainModelValidation.requireNonBlank(providerId, "providerId");
        displayName = DomainModelValidation.requireNonBlank(displayName, "displayName");
        description = description == null ? "" : description;
        Objects.requireNonNull(defaultCapabilities, "defaultCapabilities must not be null");
        models = List.copyOf(Objects.requireNonNull(models, "models must not be null"));
        extraMetadata = Map.copyOf(Objects.requireNonNull(extraMetadata, "extraMetadata must not be null"));
        for (ModelDescriptor model : models) {
            Objects.requireNonNull(model, "models must not contain null entries");
            if (!providerId.equals(model.providerId())) {
                throw new IllegalArgumentException("model providerId must match providerId");
            }
        }
    }

    public Optional<CredentialRef> optionalCredentialRef() {
        return Optional.ofNullable(credentialRef);
    }
}
