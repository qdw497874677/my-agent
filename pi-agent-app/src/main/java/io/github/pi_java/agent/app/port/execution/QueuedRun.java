package io.github.pi_java.agent.app.port.execution;

import io.github.pi_java.agent.client.run.RunProviderMetadata;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record QueuedRun(
        String runId,
        String sessionId,
        String tenantId,
        String userId,
        String workspaceId,
        String traceId,
        String correlationId,
        String inputType,
        Map<String, Object> input,
        RunProviderMetadata providerMetadata,
        Instant availableAt,
        int attemptCount) {

    public QueuedRun(
            String runId,
            String sessionId,
            String tenantId,
            String userId,
            String workspaceId,
            String traceId,
            String correlationId,
            String inputType,
            Map<String, Object> input,
            Instant availableAt,
            int attemptCount) {
        this(runId, sessionId, tenantId, userId, workspaceId, traceId, correlationId, inputType, input,
                RunProviderMetadata.EMPTY, availableAt, attemptCount);
    }

    public QueuedRun {
        requireText(runId, "runId");
        requireText(sessionId, "sessionId");
        requireText(tenantId, "tenantId");
        requireText(userId, "userId");
        requireText(workspaceId, "workspaceId");
        requireText(traceId, "traceId");
        requireText(correlationId, "correlationId");
        requireText(inputType, "inputType");
        input = Map.copyOf(Objects.requireNonNull(input, "input must not be null"));
        providerMetadata = providerMetadata == null ? RunProviderMetadata.EMPTY : providerMetadata;
        Objects.requireNonNull(availableAt, "availableAt must not be null");
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must be greater than or equal to zero");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
