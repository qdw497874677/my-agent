package io.github.pi_java.agent.client.approval;

import java.time.Instant;

public record ApprovalDecisionResponse(
        String sessionId,
        String runId,
        String approvalId,
        String toolCallId,
        ApprovalDecisionRequest.Decision decision,
        String status,
        String actorPrincipal,
        String actorRole,
        String reason,
        Instant decidedAt
) {
}
