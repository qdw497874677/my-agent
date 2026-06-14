package io.github.pi_java.agent.app.port.model;

import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ModelProviderResolution;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;
import io.github.pi_java.agent.domain.model.ProviderModelRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ModelProviderRegistry {

    List<ProviderDescriptor> listProviders();

    default List<ModelDescriptor> listModels(String providerId) {
        Objects.requireNonNull(providerId, "providerId must not be null");
        return findProvider(providerId)
                .map(ProviderDescriptor::models)
                .orElseGet(List::of);
    }

    default Optional<ModelProviderResolution> resolve(String modelRef) {
        return resolve(ProviderModelRef.parse(modelRef));
    }

    default Optional<ModelProviderResolution> resolve(ProviderModelRef modelRef) {
        Objects.requireNonNull(modelRef, "modelRef must not be null");
        return findProvider(modelRef.providerId())
                .flatMap(provider -> provider.models().stream()
                        .filter(model -> model.modelId().equals(modelRef.modelId()))
                        .findFirst()
                        .map(model -> new ModelProviderResolution(provider, model, provider.credentialRef())));
    }

    default Optional<ProviderDescriptor> findProvider(String providerId) {
        Objects.requireNonNull(providerId, "providerId must not be null");
        return listProviders().stream()
                .filter(provider -> provider.providerId().equals(providerId))
                .findFirst();
    }
}
