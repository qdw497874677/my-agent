package io.github.pi_java.agent.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelProviderRegistryContractsTest {

    @Test
    void provider_descriptor_requires_identity_and_copies_models_immutably() {
        ModelDescriptor model = modelDescriptor("gpt-4.1-mini");
        List<ModelDescriptor> mutableModels = new java.util.ArrayList<>(List.of(model));

        ProviderDescriptor provider = new ProviderDescriptor(
                "openai-compatible",
                "OpenAI Compatible",
                "Gateway-backed OpenAI-compatible provider",
                CredentialRef.of("env:OPENAI_API_KEY"),
                capabilities(),
                mutableModels,
                Map.of("baseUrl", "https://api.example.test/v1")
        );

        mutableModels.clear();

        assertThat(provider.providerId()).isEqualTo("openai-compatible");
        assertThat(provider.displayName()).isEqualTo("OpenAI Compatible");
        assertThat(provider.models()).containsExactly(model);
        assertThat(provider.models()).isUnmodifiable();
        assertThat(provider.extraMetadata()).containsEntry("baseUrl", "https://api.example.test/v1");
        assertThat(provider.extraMetadata()).isUnmodifiable();

        assertThatThrownBy(() -> new ProviderDescriptor(" ", "OpenAI", "desc", null,
                capabilities(), List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        assertThatThrownBy(() -> new ProviderDescriptor("openai", " ", "desc", null,
                capabilities(), List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
    }

    @Test
    void model_descriptor_requires_identity_and_carries_capabilities() {
        ModelCapabilities capabilities = capabilities();

        ModelDescriptor model = new ModelDescriptor(
                "openai-compatible",
                "gpt-4.1-mini",
                "GPT 4.1 Mini",
                capabilities,
                Map.of("quality", "balanced")
        );

        assertThat(model.providerId()).isEqualTo("openai-compatible");
        assertThat(model.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(model.capabilities()).isEqualTo(capabilities);
        assertThat(model.extraMetadata()).containsEntry("quality", "balanced");
        assertThat(model.extraMetadata()).isUnmodifiable();

        assertThatThrownBy(() -> new ModelDescriptor("", "gpt", "GPT", capabilities, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
        assertThatThrownBy(() -> new ModelDescriptor("openai", " ", "GPT", capabilities, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modelId");
        assertThatThrownBy(() -> new ModelDescriptor("openai", "gpt", " ", capabilities, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayName");
    }

    @Test
    void model_capabilities_cover_streaming_tools_usage_limits_and_extra_parameters() {
        ModelCapabilities capabilities = capabilities();

        assertThat(capabilities.supportsStreamingText()).isTrue();
        assertThat(capabilities.supportsToolCallIntents()).isTrue();
        assertThat(capabilities.usageReporting()).isEqualTo(ModelCapabilities.UsageReporting.AVAILABLE);
        assertThat(capabilities.contextWindowTokens()).isEqualTo(128_000);
        assertThat(capabilities.maxOutputTokens()).isEqualTo(16_384);
        assertThat(capabilities.supportsProviderExtraParameters()).isTrue();

        assertThatThrownBy(() -> new ModelCapabilities(true, true,
                ModelCapabilities.UsageReporting.AVAILABLE, 0, 1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextWindowTokens");
        assertThatThrownBy(() -> new ModelCapabilities(true, true,
                ModelCapabilities.UsageReporting.AVAILABLE, 1, -1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxOutputTokens");
    }

    @Test
    void model_provider_resolution_links_provider_model_and_credential_ref_without_raw_secret() {
        CredentialRef credentialRef = CredentialRef.of("env:OPENAI_API_KEY");
        ProviderDescriptor provider = providerDescriptor(credentialRef);
        ModelDescriptor model = modelDescriptor("gpt-4.1-mini");

        ModelProviderResolution resolution = new ModelProviderResolution(provider, model, credentialRef);

        assertThat(resolution.provider()).isEqualTo(provider);
        assertThat(resolution.model()).isEqualTo(model);
        assertThat(resolution.credentialRef()).isEqualTo(credentialRef);
        assertThat(resolution.toString()).doesNotContain("OPENAI_API_KEY");
        assertThat(resolution.toString()).doesNotContain("secret");
    }

    @Test
    void provider_model_ref_parses_explicit_provider_model_syntax() {
        ProviderModelRef ref = ProviderModelRef.parse("openai-compatible:gpt-4.1-mini");

        assertThat(ref.providerId()).isEqualTo("openai-compatible");
        assertThat(ref.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(ref.canonical()).isEqualTo("openai-compatible:gpt-4.1-mini");
        assertThat(ref.toString()).isEqualTo("openai-compatible:gpt-4.1-mini");
    }

    @Test
    void provider_model_ref_rejects_invalid_syntax() {
        assertThatThrownBy(() -> ProviderModelRef.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse(":gpt-4.1-mini")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse("openai-compatible:")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse("openai-compatible:gpt:extra")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse("openai-compatible gpt-4.1-mini")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse(" openai-compatible:gpt-4.1-mini ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse("openai-compatible: ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderModelRef.parse(" :gpt-4.1-mini")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void credential_and_secret_refs_store_references_and_redact_string_output() {
        CredentialRef credentialRef = CredentialRef.of("env:OPENAI_API_KEY");
        SecretRef secretRef = SecretRef.of("config:pi.providers.openai.api-key");

        assertThat(credentialRef.scheme()).isEqualTo("env");
        assertThat(credentialRef.ref()).isEqualTo("env:OPENAI_API_KEY");
        assertThat(secretRef.scheme()).isEqualTo("config");
        assertThat(secretRef.ref()).isEqualTo("config:pi.providers.openai.api-key");

        assertThat(credentialRef.toString()).contains("env:***");
        assertThat(credentialRef.toString()).doesNotContain("OPENAI_API_KEY");
        assertThat(secretRef.toString()).contains("config:***");
        assertThat(secretRef.toString()).doesNotContain("pi.providers.openai.api-key");
    }

    @Test
    void credential_and_secret_refs_reject_unusable_reference_syntax() {
        assertThatThrownBy(() -> CredentialRef.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CredentialRef.of(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CredentialRef.of("OPENAI_API_KEY")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CredentialRef.of("env:")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecretRef.of(":OPENAI_API_KEY")).isInstanceOf(IllegalArgumentException.class);
    }

    private static ProviderDescriptor providerDescriptor(CredentialRef credentialRef) {
        return new ProviderDescriptor(
                "openai-compatible",
                "OpenAI Compatible",
                "Gateway-backed OpenAI-compatible provider",
                credentialRef,
                capabilities(),
                List.of(modelDescriptor("gpt-4.1-mini")),
                Map.of()
        );
    }

    private static ModelDescriptor modelDescriptor(String modelId) {
        return new ModelDescriptor(
                "openai-compatible",
                modelId,
                "GPT 4.1 Mini",
                capabilities(),
                Map.of()
        );
    }

    private static ModelCapabilities capabilities() {
        return new ModelCapabilities(
                true,
                true,
                ModelCapabilities.UsageReporting.AVAILABLE,
                128_000,
                16_384,
                true
        );
    }
}
