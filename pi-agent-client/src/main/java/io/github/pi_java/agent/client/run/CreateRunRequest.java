package io.github.pi_java.agent.client.run;

import java.util.Map;

public record CreateRunRequest(
        String agentId,
        String inputType,
        Map<String, Object> input,
        String workspaceId,
        Map<String, Object> metadata) {
}
