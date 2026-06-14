package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelDescriptor;
import io.github.pi_java.agent.domain.model.ProviderDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultModelProviderQueryServiceTest {

    @Test
    void listsProvidersAndModelsThroughRegistry() {
        ModelProviderQueryService service = new DefaultModelProviderQueryService(new InMemoryRegistry(provider()));

        ModelProviderQueryService.ProviderCatalog catalog = service.listProviders(context());

        assertThat(catalog.providers()).hasSize(1);
        assertThat(catalog.providers().getFirst().providerId()).isEqualTo("openai");
        assertThat(catalog.providers().getFirst().models()).extracting(ModelDescriptor::modelId).containsExactly("gpt-4.1-mini");
        assertThat(catalog.providers().getFirst().credentialRef()).isEqualTo(CredentialRef.of("env:OPENAI_API_KEY"));
    }

    @Test
    void resolvesModelRefToProviderModelCapabilitiesAndCredentialReferenceMetadataOnly() {
        ModelProviderQueryService service = new DefaultModelProviderQueryService(new InMemoryRegistry(provider()));

        ModelProviderQueryService.ProviderModelDetails details = service.resolveModel(context(), "openai:gpt-4.1-mini").orElseThrow();

        assertThat(details.modelRef()).isEqualTo("openai:gpt-4.1-mini");
        assertThat(details.provider().providerId()).isEqualTo("openai");
        assertThat(details.model().capabilities().supportsStreamingText()).isTrue();
        assertThat(details.credentialRef()).contains(CredentialRef.of("env:OPENAI_API_KEY"));
        assertThat(details.toString()).doesNotContain("sk-live-super-secret", "OPENAI_API_KEY");
    }

    @Test
    void unknownModelRefReturnsEmpty() {
        ModelProviderQueryService service = new DefaultModelProviderQueryService(new InMemoryRegistry(provider()));

        assertThat(service.resolveModel(context(), "openai:missing")).isEmpty();
    }

    private static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static ProviderDescriptor provider() {
        ModelCapabilities capabilities = new ModelCapabilities(true, true,
                ModelCapabilities.UsageReporting.AVAILABLE, 128_000, 16_384, true);
        ModelDescriptor model = new ModelDescriptor("openai", "gpt-4.1-mini", "GPT 4.1 Mini", capabilities, Map.of());
        return new ProviderDescriptor("openai", "OpenAI Compatible", "", CredentialRef.of("env:OPENAI_API_KEY"),
                capabilities, List.of(model), Map.of());
    }

    private record InMemoryRegistry(ProviderDescriptor provider) implements ModelProviderRegistry {
        @Override
        public List<ProviderDescriptor> listProviders() {
            return List.of(provider);
        }
    }
}
