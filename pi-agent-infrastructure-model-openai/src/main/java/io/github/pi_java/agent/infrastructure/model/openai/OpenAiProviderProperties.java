package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record OpenAiProviderProperties(
        String providerId,
        String displayName,
        String baseUrl,
        String completionsPath,
        String defaultModelId,
        CredentialRef credentialRef,
        Map<String, String> defaultParameters,
        Map<String, String> extraBody,
        ModelCapabilities defaultCapabilities,
        ResilienceOptions resilience
) {
    public OpenAiProviderProperties {
        providerId = requireNonBlank(providerId, "providerId");
        displayName = requireNonBlank(displayName, "displayName");
        baseUrl = requireNonBlank(baseUrl, "baseUrl");
        completionsPath = requireNonBlank(completionsPath, "completionsPath");
        if (!completionsPath.startsWith("/")) {
            throw new IllegalArgumentException("completionsPath must start with /");
        }
        defaultModelId = requireNonBlank(defaultModelId, "defaultModelId");
        Objects.requireNonNull(credentialRef, "credentialRef must not be null");
        defaultParameters = Map.copyOf(Objects.requireNonNull(defaultParameters, "defaultParameters must not be null"));
        extraBody = Map.copyOf(Objects.requireNonNull(extraBody, "extraBody must not be null"));
        Objects.requireNonNull(defaultCapabilities, "defaultCapabilities must not be null");
        Objects.requireNonNull(resilience, "resilience must not be null");
    }

    public static OpenAiProviderProperties defaults(CredentialRef credentialRef) {
        return openAiCompatible(
                "openai-compatible",
                "https://api.openai.com/v1",
                "/chat/completions",
                "gpt-4.1-mini",
                credentialRef,
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                ResilienceOptions.defaults()
        );
    }

    public static OpenAiProviderProperties openAiCompatible(
            String providerId,
            String baseUrl,
            String completionsPath,
            String defaultModelId,
            CredentialRef credentialRef,
            Map<String, String> defaultParameters,
            Map<String, String> extraBody,
            ModelCapabilities defaultCapabilities,
            ResilienceOptions resilience
    ) {
        return new OpenAiProviderProperties(
                providerId,
                "OpenAI Compatible",
                baseUrl,
                completionsPath,
                defaultModelId,
                credentialRef,
                defaultParameters,
                extraBody,
                defaultCapabilities,
                resilience
        );
    }

    @Override
    public String toString() {
        return "OpenAiProviderProperties[providerId=" + providerId
                + ", baseUrl=" + baseUrl
                + ", completionsPath=" + completionsPath
                + ", defaultModelId=" + defaultModelId
                + ", credentialRef=" + credentialRef.redacted() + ")";
    }

    public record ResilienceOptions(
            Duration timeout,
            RetryOptions retry,
            RateLimiterOptions rateLimiter,
            CircuitBreakerOptions circuitBreaker
    ) {
        public ResilienceOptions {
            timeout = requirePositive(timeout, "timeout");
            Objects.requireNonNull(retry, "retry must not be null");
            Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
            Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
        }

        public static ResilienceOptions defaults() {
            return new ResilienceOptions(
                    Duration.ofSeconds(30),
                    new RetryOptions(false, 1, Duration.ofMillis(100)),
                    new RateLimiterOptions(false, 60, Duration.ofSeconds(1)),
                    new CircuitBreakerOptions(false, 50, 20)
            );
        }
    }

    public record RetryOptions(boolean enabled, int maxAttempts, Duration waitDuration) {
        public RetryOptions {
            requirePositive(maxAttempts, "maxAttempts");
            waitDuration = requirePositive(waitDuration, "waitDuration");
        }
    }

    public record RateLimiterOptions(boolean enabled, int limitForPeriod, Duration limitRefreshPeriod) {
        public RateLimiterOptions {
            requirePositive(limitForPeriod, "limitForPeriod");
            limitRefreshPeriod = requirePositive(limitRefreshPeriod, "limitRefreshPeriod");
        }
    }

    public record CircuitBreakerOptions(boolean enabled, int failureRateThresholdPercent, int slidingWindowSize) {
        public CircuitBreakerOptions {
            if (failureRateThresholdPercent <= 0 || failureRateThresholdPercent > 100) {
                throw new IllegalArgumentException("failureRateThresholdPercent must be between 1 and 100");
            }
            requirePositive(slidingWindowSize, "slidingWindowSize");
        }
    }

    static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
