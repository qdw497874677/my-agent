package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiProviderErrorMapperTest {

    @Test
    void mapsAuthenticationErrorsToUserActionRequired() {
        ProviderErrorSummary error = OpenAiProviderErrorMapper.fromHttpStatus(401,
                "Authorization: Bearer sk-secret-key failed", "sk-secret-key");

        assertThat(error.providerCode()).isEqualTo("provider_authentication_failed");
        assertThat(error.userActionRequired()).isTrue();
        assertThat(error.retryable()).isFalse();
        assertThat(error.safeMessage()).doesNotContain("sk-secret-key").doesNotContain("Bearer").doesNotContain("Authorization");
    }

    @Test
    void mapsRateLimitAndServerErrorsAsRetryableBeforeStreaming() {
        assertThat(OpenAiProviderErrorMapper.fromHttpStatus(429, "rate limit", null).retryable()).isTrue();
        assertThat(OpenAiProviderErrorMapper.fromHttpStatus(503, "upstream unavailable", null).retryable()).isTrue();
    }

    @Test
    void mapsProviderBadRequestCategoriesAsNonRetryable() {
        assertThat(OpenAiProviderErrorMapper.fromProviderMessage("maximum context length exceeded", null).providerCode())
                .isEqualTo("context_length_exceeded");
        assertThat(OpenAiProviderErrorMapper.fromProviderMessage("content filtered by safety system", null).providerCode())
                .isEqualTo("safety_filtered");
        assertThat(OpenAiProviderErrorMapper.fromProviderMessage("provider returned malformed response", null).providerCode())
                .isEqualTo("provider_bad_response");
    }

    @Test
    void mapsTimeoutAndCancellation() {
        assertThat(OpenAiProviderErrorMapper.timeout(Duration.ofMillis(250)).providerCode()).isEqualTo("provider_timeout");
        assertThat(OpenAiProviderErrorMapper.cancelled("user cancelled").providerCode()).isEqualTo("provider_cancelled");
    }
}
