package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.GovernanceQueryService;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/governance")
public class AdminGovernanceController {

    private final GovernanceQueryService governanceQueryService;

    public AdminGovernanceController(GovernanceQueryService governanceQueryService) {
        this.governanceQueryService = governanceQueryService;
    }

    @GetMapping({"", "/overview"})
    public GovernanceOverviewResponse overview(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.overview(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/policy-decisions")
    public List<PolicyDecisionSummaryDto> policyDecisions(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.policyDecisions(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/audits")
    public List<AuditSummaryDto> audits(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.audits(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/operations")
    public OperationsSummaryResponse operations(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.operations(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/extensions")
    public ExtensionGovernanceResponse extensions(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.extensions(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/mcp")
    public McpGovernanceResponse mcp(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.mcp(SessionController.toRequestContext(principal, servletRequest));
    }

    @PostMapping("/mcp/refresh")
    public McpRefreshResponse refreshMcp(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.refreshMcp(SessionController.toRequestContext(principal, servletRequest));
    }

    @GetMapping("/plugins")
    public PluginGovernanceResponse plugins(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.plugins(SessionController.toRequestContext(principal, servletRequest));
    }

    @PostMapping("/plugins/refresh")
    public PluginMutationResponse refreshPlugins(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.refreshPlugins(SessionController.toRequestContext(principal, servletRequest));
    }

    @PostMapping("/plugins/{pluginId}/disable")
    public PluginMutationResponse disablePlugin(@PathVariable("pluginId") String pluginId,
                                                @RequestBody(required = false) PluginMutationRequest request,
                                                Principal principal,
                                                HttpServletRequest servletRequest) {
        PluginMutationRequest safeRequest = request == null ? new PluginMutationRequest("disable", "") : request;
        return governanceQueryService.disablePlugin(SessionController.toRequestContext(principal, servletRequest),
                pluginId, safeRequest);
    }

    @PostMapping("/plugins/{pluginId}/quarantine")
    public PluginMutationResponse quarantinePlugin(@PathVariable("pluginId") String pluginId,
                                                   @RequestBody(required = false) PluginMutationRequest request,
                                                   Principal principal,
                                                   HttpServletRequest servletRequest) {
        PluginMutationRequest safeRequest = request == null ? new PluginMutationRequest("quarantine", "") : request;
        return governanceQueryService.quarantinePlugin(SessionController.toRequestContext(principal, servletRequest),
                pluginId, safeRequest);
    }
}
