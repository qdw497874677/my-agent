package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.domain.model.ModelProviderResolution;

import java.util.Objects;
import java.util.Optional;

public final class DefaultModelProviderQueryService implements ModelProviderQueryService {
    private final ModelProviderRegistry registry;

    public DefaultModelProviderQueryService(ModelProviderRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    @Override
    public ProviderCatalog listProviders(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new ProviderCatalog(registry.listProviders().stream()
                .map(provider -> new ProviderSummary(provider, provider.models(), provider.credentialRef()))
                .toList());
    }

    @Override
    public Optional<ProviderModelDetails> resolveModel(RequestContext context, String modelRef) {
        Objects.requireNonNull(context, "context must not be null");
        return registry.resolve(modelRef)
                .map(resolution -> new ProviderModelDetails(
                        modelRef,
                        resolution.provider(),
                        resolution.model(),
                        resolution.optionalCredentialRef()))
                .map(DefaultModelProviderQueryService::withoutRawSecretMaterial);
    }

    private static ProviderModelDetails withoutRawSecretMaterial(ProviderModelDetails details) {
        return details;
    }
}
