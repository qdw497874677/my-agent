package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;

public interface ApprovalCommandService {
    ApprovalDecisionResponse decide(RequestContext context, String sessionId, String runId, String approvalId,
                                    ApprovalDecisionRequest request);
}
