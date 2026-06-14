package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tools")
public class ToolRegistryController {

    private final ToolRegistryQueryService toolRegistryQueryService;

    public ToolRegistryController(ToolRegistryQueryService toolRegistryQueryService) {
        this.toolRegistryQueryService = toolRegistryQueryService;
    }

    @GetMapping
    public ToolCatalogResponse listTools(Principal principal, HttpServletRequest servletRequest) {
        return toolRegistryQueryService.listTools(SessionController.toRequestContext(principal, servletRequest));
    }
}
