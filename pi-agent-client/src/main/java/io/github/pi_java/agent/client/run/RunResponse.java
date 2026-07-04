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
        Instant updatedAt,
        RunProviderMetadata providerMetadata) {

    public RunResponse(String tenantId, String userId, String sessionId, String runId,
                       String workspaceId, String status, String traceId, String correlationId,
                       Instant createdAt, Instant updatedAt) {
        this(tenantId, userId, sessionId, runId, workspaceId, status, traceId, correlationId,
                createdAt, updatedAt, RunProviderMetadata.EMPTY);
    }

    public RunResponse {
        providerMetadata = providerMetadata == null ? RunProviderMetadata.EMPTY : providerMetadata;
    }
}
