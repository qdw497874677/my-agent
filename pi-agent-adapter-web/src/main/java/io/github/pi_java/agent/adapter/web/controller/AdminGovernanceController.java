package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.GovernanceQueryService;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/extensions")
    public ExtensionGovernanceResponse extensions(Principal principal, HttpServletRequest servletRequest) {
        return governanceQueryService.extensions(SessionController.toRequestContext(principal, servletRequest));
    }
}
