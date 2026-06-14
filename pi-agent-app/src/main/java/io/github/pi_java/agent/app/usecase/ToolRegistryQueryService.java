package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;

public interface ToolRegistryQueryService {

    ToolCatalogResponse listTools(RequestContext context);
}
