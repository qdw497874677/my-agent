package io.github.pi_java.agent.app.port;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ModelProviderResolution;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.SecretRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelProviderAppPortContractTest {

    @Test
    void registryListsAndResolvesProviderModelReferencesWithoutNulls() {
        ModelProviderRegistry registry = new InMemoryRegistry(provider());

        assertThat(registry.listProviders()).extracting(ProviderDescriptor::providerId).containsExactly("openai");
        assertThat(registry.listModels("openai")).extracting(ModelDescriptor::modelId).containsExactly("gpt-4.1-mini");

        Optional<ModelProviderResolution> resolved = registry.resolve(ProviderModelRef.parse("openai:gpt-4.1-mini"));

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().provider().providerId()).isEqualTo("openai");
        assertThat(resolved.orElseThrow().model().modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(resolved.orElseThrow().credentialRef().redacted()).isEqualTo("env:***");
    }

    @Test
    void registryResolutionReturnsEmptyForUnknownProviderOrModel() {
        ModelProviderRegistry registry = new InMemoryRegistry(provider());

        assertThat(registry.resolve(ProviderModelRef.parse("missing:gpt-4.1-mini"))).isEmpty();
        assertThat(registry.resolve(ProviderModelRef.parse("openai:missing"))).isEmpty();
        assertThat(registry.listModels("missing")).isEmpty();
    }

    @Test
    void secretResolverReturnsRawValueButNeverPrintsIt() {
        SecretResolver resolver = new InMemorySecretResolver("sk-live-super-secret");

        ResolvedSecret resolved = resolver.resolve(CredentialRef.of("env:OPENAI_API_KEY"));

        assertThat(resolved.rawValue()).isEqualTo("sk-live-super-secret");
        assertThat(resolved.redactedDisplay()).isEqualTo("env:***");
        assertThat(resolved.toString()).doesNotContain("sk-live-super-secret", "OPENAI_API_KEY");
    }

    @Test
    void resolvedSecretValidationDoesNotLeakRawValueInExceptionMessages() {
        assertThatThrownBy(() -> new ResolvedSecret(SecretRef.of("env:OPENAI_API_KEY"), "sk-live-super-secret", "value=sk-live-super-secret"))
                .hasMessageContaining("rawValue must be marked sensitive")
                .hasMessageNotContaining("sk-live-super-secret")
                .hasMessageNotContaining("OPENAI_API_KEY");
    }

    private static ProviderDescriptor provider() {
        ModelCapabilities capabilities = new ModelCapabilities(true, true,
                ModelCapabilities.UsageReporting.AVAILABLE, 128_000, 16_384, true);
        ModelDescriptor model = new ModelDescriptor("openai", "gpt-4.1-mini", "GPT 4.1 Mini", capabilities, Map.of());
        return new ProviderDescriptor("openai", "OpenAI Compatible", "", CredentialRef.of("env:OPENAI_API_KEY"),
                capabilities, List.of(model), Map.of("baseUrl", "https://api.example.test/v1"));
    }

    private record InMemoryRegistry(ProviderDescriptor provider) implements ModelProviderRegistry {
        @Override
        public List<ProviderDescriptor> listProviders() {
            return List.of(provider);
        }
    }

    private record InMemorySecretResolver(String rawValue) implements SecretResolver {
        @Override
        public Optional<ResolvedSecret> resolve(SecretRef secretRef) {
            return Optional.of(ResolvedSecret.sensitive(secretRef, rawValue));
        }
    }
}
