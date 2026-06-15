package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.ApprovalCommandService;
import io.github.pi_java.agent.app.usecase.ApprovalQueryService;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/runs/{runId}/approvals")
public class ApprovalController {

    private final ApprovalQueryService approvalQueryService;
    private final ApprovalCommandService approvalCommandService;

    public ApprovalController(
            @Qualifier("approvalQueryService") ApprovalQueryService approvalQueryService,
            @Qualifier("approvalCommandService") ApprovalCommandService approvalCommandService) {
        this.approvalQueryService = approvalQueryService;
        this.approvalCommandService = approvalCommandService;
    }

    @GetMapping
    public List<ApprovalSummaryDto> listApprovals(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId) {
        return approvalQueryService.listPendingApprovals(SessionController.toRequestContext(principal, servletRequest), sessionId, runId);
    }

    @PostMapping("/{approvalId}/decision")
    public ApprovalDecisionResponse decide(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @PathVariable("approvalId") String approvalId,
            @RequestBody ApprovalDecisionRequest request) {
        return approvalCommandService.decide(SessionController.toRequestContext(principal, servletRequest), sessionId, runId,
                approvalId, request);
    }
}
