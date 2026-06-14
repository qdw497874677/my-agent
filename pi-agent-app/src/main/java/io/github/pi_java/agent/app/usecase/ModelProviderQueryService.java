package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ModelProviderQueryService {

    ProviderCatalog listProviders(RequestContext context);

    Optional<ProviderModelDetails> resolveModel(RequestContext context, String modelRef);

    record ProviderCatalog(List<ProviderSummary> providers) {
        public ProviderCatalog {
            providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
        }
    }

    record ProviderSummary(ProviderDescriptor provider, List<ModelDescriptor> models, CredentialRef credentialRef) {
        public ProviderSummary {
            Objects.requireNonNull(provider, "provider must not be null");
            models = List.copyOf(Objects.requireNonNull(models, "models must not be null"));
        }

        public String providerId() {
            return provider.providerId();
        }

        @Override
        public String toString() {
            return "ProviderSummary[providerId=" + provider.providerId()
                    + ", models=" + models.size()
                    + ", credentialRef=" + (credentialRef == null ? "<none>" : credentialRef.redacted())
                    + "]";
        }
    }

    record ProviderModelDetails(String modelRef, ProviderDescriptor provider, ModelDescriptor model,
                                Optional<CredentialRef> credentialRef) {
        public ProviderModelDetails {
            modelRef = requireNonBlank(modelRef, "modelRef");
            Objects.requireNonNull(provider, "provider must not be null");
            Objects.requireNonNull(model, "model must not be null");
            credentialRef = Objects.requireNonNull(credentialRef, "credentialRef must not be null");
        }

        @Override
        public String toString() {
            return "ProviderModelDetails[modelRef=" + modelRef
                    + ", providerId=" + provider.providerId()
                    + ", modelId=" + model.modelId()
                    + ", credentialRef=" + credentialRef.map(CredentialRef::redacted).orElse("<none>")
                    + "]";
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
