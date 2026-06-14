package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InMemoryModelProviderRegistry implements ModelProviderRegistry {
    private final List<ProviderDescriptor> providers;

    public InMemoryModelProviderRegistry(List<ProviderDescriptor> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
        this.providers.forEach(provider -> Objects.requireNonNull(provider, "providers must not contain null entries"));
    }

    public static InMemoryModelProviderRegistry fromProperties(List<OpenAiProviderProperties> properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        return new InMemoryModelProviderRegistry(properties.stream()
                .map(InMemoryModelProviderRegistry::toProviderDescriptor)
                .toList());
    }

    @Override
    public List<ProviderDescriptor> listProviders() {
        return providers;
    }

    private static ProviderDescriptor toProviderDescriptor(OpenAiProviderProperties properties) {
        Objects.requireNonNull(properties, "properties must not contain null entries");
        ModelDescriptor defaultModel = new ModelDescriptor(
                properties.providerId(),
                properties.defaultModelId(),
                properties.defaultModelId(),
                properties.defaultCapabilities(),
                Map.of(
                        "baseUrl", properties.baseUrl(),
                        "completionsPath", properties.completionsPath()
                )
        );
        return new ProviderDescriptor(
                properties.providerId(),
                properties.displayName(),
                "OpenAI-compatible chat completions provider",
                properties.credentialRef(),
                properties.defaultCapabilities(),
                List.of(defaultModel),
                Map.of(
                        "baseUrl", properties.baseUrl(),
                        "completionsPath", properties.completionsPath()
                )
        );
    }
}
