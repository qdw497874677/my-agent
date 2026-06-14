package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;

import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface ToolPreviewGenerator {

    ProvisionPreview generate(PreviewRequest request);

    record PreviewRequest(
            RequestContext context,
            SessionId sessionId,
            WorkspaceId workspaceId,
            ToolDescriptor descriptor,
            ToolExecutionRequest toolRequest,
            ToolPolicyEvaluator.PolicyEvaluation policyEvaluation,
            Map<String, Object> redactedArguments
    ) {
        public PreviewRequest {
            Objects.requireNonNull(context, "context must not be null");
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(workspaceId, "workspaceId must not be null");
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            Objects.requireNonNull(toolRequest, "toolRequest must not be null");
            Objects.requireNonNull(policyEvaluation, "policyEvaluation must not be null");
            redactedArguments = Map.copyOf(Objects.requireNonNull(redactedArguments, "redactedArguments must not be null"));
        }
    }
}
