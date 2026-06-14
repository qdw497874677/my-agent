package io.github.pi_java.agent.client.session;

import java.time.Instant;
import java.util.Map;

public record SessionResponse(
        String tenantId,
        String userId,
        String sessionId,
        String workspaceId,
        String currentEntryId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> metadata) {
}
