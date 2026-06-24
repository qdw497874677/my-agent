package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.ApprovalCommandService;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
import java.util.Objects;
import java.util.Set;

/** Adapter-web bridge from Vaadin approval clicks to the App-layer approval command use case. */
public final class AppApprovalDecisionHandler implements ApprovalDecisionHandler {

    private final ApprovalCommandService approvalCommandService;

    public AppApprovalDecisionHandler(ApprovalCommandService approvalCommandService) {
        this.approvalCommandService = Objects.requireNonNull(approvalCommandService, "approvalCommandService must not be null");
    }

    @Override
    public ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        return approvalCommandService.decide(
                context(plan.request().actorRole()),
                plan.sessionId(),
                plan.runId(),
                plan.approvalId(),
                plan.request());
    }

    private static RequestContext context(String actorRole) {
        String normalized = actorRole == null ? "USER" : actorRole.trim().toUpperCase();
        Set<String> authorities = "ADMIN".equals(normalized) ? Set.of("ROLE_ADMIN") : Set.of("ROLE_USER");
        return new RequestContext(
                new SecurityPrincipalContext("vaadin-approval", "vaadin-approval", authorities),
                new CorrelationContext("vaadin-approval", "vaadin-approval", "vaadin-approval"));
    }
}
