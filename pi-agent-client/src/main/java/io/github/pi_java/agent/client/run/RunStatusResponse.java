package io.github.pi_java.agent.client.run;

import java.time.Instant;

public record RunStatusResponse(
        String sessionId,
        String runId,
        String status,
        boolean terminal,
        Instant updatedAt,
        String traceId,
        String correlationId) {
}
