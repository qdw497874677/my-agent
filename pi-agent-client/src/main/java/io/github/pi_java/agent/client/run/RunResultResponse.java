package io.github.pi_java.agent.client.run;

import java.util.Map;

public record RunResultResponse(
        String runId,
        String status,
        Map<String, Object> terminalResult,
        Map<String, Object> failure) {
}
