package io.github.pi_java.agent.client.run;

import java.time.Instant;

public record RunResponse(
        String tenantId,
        String userId,
        String sessionId,
        String runId,
        String workspaceId,
        String status,
        String traceId,
        String correlationId,
        Instant createdAt,
        Instant updatedAt) {
}
