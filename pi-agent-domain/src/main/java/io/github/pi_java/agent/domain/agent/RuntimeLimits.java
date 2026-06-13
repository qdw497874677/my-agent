package io.github.pi_java.agent.domain.agent;

import java.time.Duration;
import java.util.Objects;

public record RuntimeLimits(Duration deadline, int maxSteps, int maxToolCalls) {

    public RuntimeLimits {
        Objects.requireNonNull(deadline, "deadline must not be null");
        if (deadline.isZero() || deadline.isNegative()) {
            throw new IllegalArgumentException("deadline must be positive");
        }
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        if (maxToolCalls <= 0) {
            throw new IllegalArgumentException("maxToolCalls must be positive");
        }
    }
}
