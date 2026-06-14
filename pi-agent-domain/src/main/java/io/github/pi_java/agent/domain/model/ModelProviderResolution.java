package io.github.pi_java.agent.domain.model;

import java.util.Objects;
import java.util.Optional;

public record ModelProviderResolution(
        ProviderDescriptor provider,
        ModelDescriptor model,
        CredentialRef credentialRef
) {
    public ModelProviderResolution {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(model, "model must not be null");
        if (!provider.providerId().equals(model.providerId())) {
            throw new IllegalArgumentException("model providerId must match resolved provider");
        }
    }

    public Optional<CredentialRef> optionalCredentialRef() {
        return Optional.ofNullable(credentialRef);
    }

    @Override
    public String toString() {
        return "ModelProviderResolution[providerId=" + provider.providerId()
                + ", modelId=" + model.modelId()
                + ", credentialRef=" + (credentialRef == null ? "<none>" : credentialRef.redacted())
                + "]";
    }
}
