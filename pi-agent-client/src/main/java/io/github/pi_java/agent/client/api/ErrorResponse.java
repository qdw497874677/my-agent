package io.github.pi_java.agent.client.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details) {
}
