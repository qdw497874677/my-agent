package io.github.pi_java.agent.client.approval;

import java.util.Map;
import java.util.Set;

public record ApprovalSummaryDto(
        String sessionId,
        String runId,
        String approvalId,
        String toolCallId,
        String toolId,
        String toolName,
        String policyReason,
        String riskLabel,
        String sideEffectLabel,
        Map<String, Object> provisionPreview,
        Map<String, Object> redactedArgumentSummary,
        String expectedConsequence,
        boolean actorEligible,
        Set<String> eligibleActorRoles
) {
    public ApprovalSummaryDto {
        provisionPreview = provisionPreview == null ? Map.of() : Map.copyOf(provisionPreview);
        redactedArgumentSummary = redactedArgumentSummary == null ? Map.of() : Map.copyOf(redactedArgumentSummary);
        eligibleActorRoles = eligibleActorRoles == null ? Set.of() : Set.copyOf(eligibleActorRoles);
    }
}
