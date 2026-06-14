package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiProviderPropertiesTest {

    @Test
    void createsValidatedDefaultOpenAiCompatibleProviderConfiguration() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.openAiCompatible(
                "openai-compatible",
                "https://gateway.example.test/v1",
                "/chat/completions",
                "gpt-4.1-mini",
                CredentialRef.of("env:OPENAI_API_KEY"),
                Map.of("temperature", "0.2"),
                Map.of("reasoning_effort", "low"),
                new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                new OpenAiProviderProperties.ResilienceOptions(
                        Duration.ofSeconds(45),
                        new OpenAiProviderProperties.RetryOptions(true, 2, Duration.ofMillis(200)),
                        new OpenAiProviderProperties.RateLimiterOptions(true, 60, Duration.ofSeconds(1)),
                        new OpenAiProviderProperties.CircuitBreakerOptions(true, 50, 20)
                )
        );

        assertThat(properties.providerId()).isEqualTo("openai-compatible");
        assertThat(properties.baseUrl()).isEqualTo("https://gateway.example.test/v1");
        assertThat(properties.completionsPath()).isEqualTo("/chat/completions");
        assertThat(properties.defaultModelId()).isEqualTo("gpt-4.1-mini");
        assertThat(properties.defaultParameters()).containsEntry("temperature", "0.2");
        assertThat(properties.extraBody()).containsEntry("reasoning_effort", "low");
        assertThat(properties.resilience().timeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(properties.credentialRef().redacted()).isEqualTo("env:***");
    }

    @Test
    void rejectsBlankRequiredProviderConfiguration() {
        assertThatThrownBy(() -> OpenAiProviderProperties.openAiCompatible(
                " ",
                "https://gateway.example.test/v1",
                "/chat/completions",
                "gpt-4.1-mini",
                CredentialRef.of("env:OPENAI_API_KEY"),
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, false, ModelCapabilities.UsageReporting.UNKNOWN, 8_192, 1_024, true),
                OpenAiProviderProperties.ResilienceOptions.defaults()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId");
    }

    @Test
    void stringOutputRedactsCredentialReferenceTarget() {
        OpenAiProviderProperties properties = OpenAiProviderProperties.defaults(CredentialRef.of("config:pi.providers.openai.api-key"));

        assertThat(properties.toString())
                .contains("config:***)")
                .doesNotContain("pi.providers.openai.api-key");
    }
}
