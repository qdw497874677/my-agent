package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelProviderResolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryModelProviderRegistryTest {

    @Test
    void exposesProviderAndDefaultModelFromProperties() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.defaults(CredentialRef.of("env:OPENAI_API_KEY"));
        InMemoryModelProviderRegistry registry = InMemoryModelProviderRegistry.fromProperties(List.of(properties));

        assertThat(registry.listProviders()).hasSize(1);
        assertThat(registry.listModels("openai-compatible")).singleElement()
                .satisfies(model -> {
                    assertThat(model.providerId()).isEqualTo("openai-compatible");
                    assertThat(model.modelId()).isEqualTo("gpt-4.1-mini");
                    assertThat(model.capabilities().supportsStreamingText()).isTrue();
                    assertThat(model.capabilities().supportsProviderExtraParameters()).isTrue();
                });

        ModelProviderResolution resolution = registry.resolve("openai-compatible:gpt-4.1-mini").orElseThrow();
        assertThat(resolution.provider().providerId()).isEqualTo("openai-compatible");
        assertThat(resolution.credentialRef().redacted()).isEqualTo("env:***");
    }

    @Test
    void returnsEmptyForUnknownProviderOrModel() {
        InMemoryModelProviderRegistry registry = InMemoryModelProviderRegistry.fromProperties(
                List.of(OpenAiProviderProperties.defaults(CredentialRef.of("env:OPENAI_API_KEY")))
        );

        assertThat(registry.resolve("missing:gpt-4.1-mini")).isEmpty();
        assertThat(registry.resolve("openai-compatible:missing-model")).isEmpty();
        assertThat(registry.listModels("missing")).isEmpty();
    }
}
