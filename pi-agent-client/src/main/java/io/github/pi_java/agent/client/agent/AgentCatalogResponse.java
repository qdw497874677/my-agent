package io.github.pi_java.agent.client.agent;

import java.util.List;

public record AgentCatalogResponse(List<AgentCatalogItemDto> agents) {
    public AgentCatalogResponse {
        agents = List.copyOf(agents);
    }
}
