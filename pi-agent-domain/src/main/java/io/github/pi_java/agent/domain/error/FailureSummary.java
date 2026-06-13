package io.github.pi_java.agent.domain.error;

import java.util.Objects;

public record FailureSummary(String message, PiError error) {

    public FailureSummary {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        Objects.requireNonNull(error, "error must not be null");
    }
}
