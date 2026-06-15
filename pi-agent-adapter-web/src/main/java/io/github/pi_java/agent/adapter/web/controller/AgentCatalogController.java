package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentCatalogController {

    private final AgentCatalogQueryService agentCatalogQueryService;

    public AgentCatalogController(AgentCatalogQueryService agentCatalogQueryService) {
        this.agentCatalogQueryService = agentCatalogQueryService;
    }

    @GetMapping
    public AgentCatalogResponse listAgents(Principal principal, HttpServletRequest servletRequest) {
        return agentCatalogQueryService.listAgents(SessionController.toRequestContext(principal, servletRequest));
    }
}
