package io.github.pi_java.agent.client.admin;

import java.util.List;

public record McpGovernanceResponse(List<McpServerDto> servers) {
    public McpGovernanceResponse {
        servers = List.copyOf(servers == null ? List.of() : servers);
    }
}
