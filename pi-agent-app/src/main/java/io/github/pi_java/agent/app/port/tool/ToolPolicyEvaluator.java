package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ToolPolicyEvaluator {

    PolicyEvaluation evaluate(PolicyEvaluationRequest request);

    record PolicyEvaluationRequest(
            RequestContext context,
            SessionId sessionId,
            WorkspaceId workspaceId,
            ToolDescriptor descriptor,
            ToolExecutionRequest toolRequest,
            Map<String, Object> redactedArguments
    ) {
        public PolicyEvaluationRequest {
            Objects.requireNonNull(context, "context must not be null");
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(workspaceId, "workspaceId must not be null");
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            Objects.requireNonNull(toolRequest, "toolRequest must not be null");
            redactedArguments = Map.copyOf(Objects.requireNonNull(redactedArguments, "redactedArguments must not be null"));
        }
    }

    record PolicyEvaluation(
            PolicyDecision decision,
            String reason,
            String policyRef,
            boolean previewRequired,
            Optional<String> approvalRef,
            Optional<String> sandboxRef,
            Map<String, Object> redactedMetadata
    ) {
        public PolicyEvaluation(
                PolicyDecision decision,
                String reason,
                String policyRef,
                boolean previewRequired,
                String approvalRef,
                String sandboxRef,
                Map<String, Object> redactedMetadata
        ) {
            this(decision, reason, policyRef, previewRequired, Optional.ofNullable(approvalRef),
                    Optional.ofNullable(sandboxRef), redactedMetadata);
        }

        public PolicyEvaluation {
            Objects.requireNonNull(decision, "decision must not be null");
            reason = Objects.requireNonNull(reason, "reason must not be null");
            policyRef = requireNonBlank(policyRef, "policyRef");
            approvalRef = Objects.requireNonNull(approvalRef, "approvalRef must not be null");
            approvalRef.ifPresent(value -> requireNonBlank(value, "approvalRef"));
            sandboxRef = Objects.requireNonNull(sandboxRef, "sandboxRef must not be null");
            sandboxRef.ifPresent(value -> requireNonBlank(value, "sandboxRef"));
            redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
        }

        public static PolicyEvaluation allow(String reason, String policyRef) {
            return new PolicyEvaluation(PolicyDecision.ALLOW, reason, policyRef, false, Optional.empty(), Optional.empty(), Map.of());
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
