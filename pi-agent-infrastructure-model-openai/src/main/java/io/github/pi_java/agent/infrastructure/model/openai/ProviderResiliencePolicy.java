package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.util.function.Supplier;

public final class ProviderResiliencePolicy {
    private final OpenAiProviderProperties.ResilienceOptions options;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public ProviderResiliencePolicy(String providerId, OpenAiProviderProperties.ResilienceOptions options) {
        this.options = options;
        this.retry = Retry.of(providerId + "-model", RetryConfig.custom()
                .maxAttempts(options.retry().enabled() ? options.retry().maxAttempts() : 1)
                .waitDuration(options.retry().waitDuration())
                .build());
        this.rateLimiter = RateLimiter.of(providerId + "-model", RateLimiterConfig.custom()
                .limitForPeriod(options.rateLimiter().limitForPeriod())
                .limitRefreshPeriod(options.rateLimiter().limitRefreshPeriod())
                .timeoutDuration(options.rateLimiter().enabled() ? options.rateLimiter().limitRefreshPeriod() : java.time.Duration.ZERO)
                .build());
        this.circuitBreaker = CircuitBreaker.of(providerId + "-model", CircuitBreakerConfig.custom()
                .failureRateThreshold(options.circuitBreaker().failureRateThresholdPercent())
                .slidingWindowSize(options.circuitBreaker().slidingWindowSize())
                .build());
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom().timeoutDuration(options.timeout()).build());
    }

    public <T> T executeBeforeStream(Supplier<T> supplier) {
        Supplier<T> decorated = supplier;
        if (options.circuitBreaker().enabled()) {
            decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        }
        if (options.rateLimiter().enabled()) {
            decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
        }
        if (options.retry().enabled()) {
            decorated = Retry.decorateSupplier(retry, decorated);
        }
        return decorated.get();
    }

    public java.time.Duration timeout() {
        return options.timeout();
    }

    public TimeLimiter timeLimiter() {
        return timeLimiter;
    }
}
