package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;

public interface AgentCatalogQueryService {

    AgentCatalogResponse listAgents(RequestContext context);
}
