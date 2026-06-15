package io.github.pi_java.agent.client.approval;

public record ApprovalDecisionRequest(Decision decision, String reason, String actorRole) {

    public ApprovalDecisionRequest {
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null");
        }
        reason = reason == null ? "" : reason;
        actorRole = actorRole == null || actorRole.isBlank() ? "USER" : actorRole;
    }

    public enum Decision {
        APPROVE,
        REJECT
    }
}
