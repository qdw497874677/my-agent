package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.error.FailureSummary;

import java.util.Objects;
import java.util.Optional;

public record RunHandle(String runId, RunStatus status, Optional<FailureSummary> failureSummary) {
    public RunHandle {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        failureSummary = Objects.requireNonNull(failureSummary, "failureSummary must not be null");
    }
}
