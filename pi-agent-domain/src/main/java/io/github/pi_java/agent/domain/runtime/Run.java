package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.error.FailureSummary;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Run(
        RunId runId,
        TenantId tenantId,
        UserId userId,
        SessionId sessionId,
        WorkspaceId workspaceId,
        RunStatus status,
        Instant createdAt,
        Instant updatedAt,
        FailureSummary failureSummary
) {

    public Run {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public Optional<FailureSummary> optionalFailureSummary() {
        return Optional.ofNullable(failureSummary);
    }
}
