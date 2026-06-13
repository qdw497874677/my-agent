package io.github.pi_java.agent.domain.event;

import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;

import java.time.Instant;
import java.util.Objects;

public record RunEvent(
        String eventId,
        TenantId tenantId,
        UserId userId,
        SessionId sessionId,
        RunId runId,
        StepId stepId,
        WorkspaceId workspaceId,
        long sequence,
        Instant timestamp,
        RunEventType type,
        TraceId traceId,
        CorrelationId correlationId,
        CausationId causationId,
        RunEventPayload payload,
        EventVisibility visibility,
        RedactionMetadata redaction
) {

    public RunEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be greater than or equal to zero");
        }
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(causationId, "causationId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
        Objects.requireNonNull(redaction, "redaction must not be null");
    }
}
