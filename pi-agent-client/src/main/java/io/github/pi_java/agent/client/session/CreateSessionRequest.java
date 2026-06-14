package io.github.pi_java.agent.client.session;

import java.util.Map;

public record CreateSessionRequest(
        String workspaceId,
        Map<String, Object> metadata) {
}
