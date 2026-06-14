package io.github.pi_java.agent.client.event;

import java.time.Instant;
import java.util.Map;

public record RunEventDto(
        String eventId,
        String tenantId,
        String userId,
        String sessionId,
        String runId,
        String stepId,
        String workspaceId,
        long sequence,
        Instant timestamp,
        String type,
        String traceId,
        String correlationId,
        String causationId,
        String visibility,
        RedactionDto redaction,
        String payloadSchema,
        int payloadVersion,
        Map<String, Object> payload
) {
}
