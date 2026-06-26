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

    @Test
    void extractsHttpStatusFromCommonProviderExceptionShapesAndCauses() {
        ProviderErrorSummary rawStatus = OpenAiProviderErrorMapper.fromThrowable(
                new RawStatusException(429, "rate limit"), null, true);
        assertThat(rawStatus.providerCode()).isEqualTo("provider_rate_limited");
        assertThat(rawStatus.httpStatus()).isEqualTo(429);

        ProviderErrorSummary statusCodeObject = OpenAiProviderErrorMapper.fromThrowable(
                new StatusCodeException(new StatusCode(401), "unauthorized"), null, true);
        assertThat(statusCodeObject.providerCode()).isEqualTo("provider_authentication_failed");
        assertThat(statusCodeObject.httpStatus()).isEqualTo(401);

        ProviderErrorSummary wrapped = OpenAiProviderErrorMapper.fromThrowable(
                new RuntimeException("wrapper", new StatusMethodException(503, "unavailable")), null, true);
        assertThat(wrapped.providerCode()).isEqualTo("provider_transient_failure");
        assertThat(wrapped.httpStatus()).isEqualTo(503);
    }

    private static final class RawStatusException extends RuntimeException {
        private final int status;

        private RawStatusException(int status, String message) {
            super(message);
            this.status = status;
        }

        int getRawStatusCode() {
            return status;
        }
    }

    private static final class StatusCodeException extends RuntimeException {
        private final StatusCode statusCode;

        private StatusCodeException(StatusCode statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        StatusCode getStatusCode() {
            return statusCode;
        }
    }

    private record StatusCode(int value) {
    }

    private static final class StatusMethodException extends RuntimeException {
        private final int status;

        private StatusMethodException(int status, String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
