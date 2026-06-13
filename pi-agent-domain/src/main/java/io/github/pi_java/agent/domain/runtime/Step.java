package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.FailureSummary;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Step(
        StepId stepId,
        RunId runId,
        StepStatus status,
        String kind,
        Instant startedAt,
        Instant endedAt,
        FailureSummary failureSummary
) {

    public Step {
        Objects.requireNonNull(stepId, "stepId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public Optional<FailureSummary> optionalFailureSummary() {
        return Optional.ofNullable(failureSummary);
    }
}
