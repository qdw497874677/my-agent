package io.github.pi_java.agent.domain.error;

import io.github.pi_java.agent.domain.event.EventVisibility;

import java.util.Objects;

public record PiError(
        Category category,
        String code,
        Severity severity,
        EventVisibility visibility,
        boolean retryable,
        boolean recoverable,
        boolean userActionRequired
) {

    public PiError {
        Objects.requireNonNull(category, "category must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
    }

    public enum Category {
        RUNTIME,
        MODEL,
        TOOL,
        POLICY,
        WORKSPACE,
        VALIDATION,
        CANCELLATION,
        TIMEOUT,
        INTERNAL
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        FATAL
    }
}
