package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.error.PiError;

import java.util.Objects;

public record ProviderErrorSummary(
        PiError piError,
        String providerCode,
        String safeMessage,
        Integer httpStatus,
        boolean retryable,
        boolean recoverable,
        boolean userActionRequired
) {
    public ProviderErrorSummary {
        Objects.requireNonNull(piError, "piError must not be null");
        if (piError.category() != PiError.Category.MODEL) {
            throw new IllegalArgumentException("provider model errors must map to PiError.Category.MODEL");
        }
        providerCode = providerCode == null || providerCode.isBlank() ? piError.code() : providerCode;
        safeMessage = requireSafeMessage(safeMessage);
        if (httpStatus != null && (httpStatus < 100 || httpStatus > 599)) {
            throw new IllegalArgumentException("httpStatus must be a valid HTTP status code");
        }
    }

    private static String requireSafeMessage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("safeMessage must not be blank");
        }
        String lower = value.toLowerCase();
        if (lower.contains("authorization") || lower.contains("api_key") || lower.contains("apikey")
                || lower.contains("api-key") || lower.contains("bearer ") || lower.contains("sk-")) {
            throw new IllegalArgumentException("safeMessage must not contain raw secrets or authorization material");
        }
        return value;
    }
}
