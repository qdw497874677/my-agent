package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
import java.time.Instant;

/** Callback used by Vaadin approval cards to execute an approval decision. */
@FunctionalInterface
public interface ApprovalDecisionHandler {

    ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan);

    static ApprovalDecisionHandler demo() {
        return plan -> new ApprovalDecisionResponse(
                plan.sessionId(),
                plan.runId(),
                plan.approvalId(),
                plan.toolCallId(),
                plan.request().decision(),
                plan.request().decision() == ApprovalDecisionRequest.Decision.APPROVE ? "APPROVED" : "REJECTED",
                "vaadin-demo",
                plan.request().actorRole(),
                plan.request().reason(),
                Instant.now());
    }
}
