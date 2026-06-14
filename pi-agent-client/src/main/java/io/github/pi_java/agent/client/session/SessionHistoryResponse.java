package io.github.pi_java.agent.client.session;

import java.util.List;
import java.util.Map;

public record SessionHistoryResponse(
        SessionResponse session,
        List<Map<String, Object>> entries) {
}
