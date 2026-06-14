package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;

import java.util.Objects;

public interface ToolExecutionGateway {

    ToolExecutionResult execute(ToolExecutionCommand command);

    record ToolExecutionCommand(
            RequestContext context,
            SessionId sessionId,
            WorkspaceId workspaceId,
            ToolExecutionRequest request,
            CancellationToken cancellationToken
    ) {
        public ToolExecutionCommand {
            Objects.requireNonNull(context, "context must not be null");
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(workspaceId, "workspaceId must not be null");
            Objects.requireNonNull(request, "request must not be null");
            cancellationToken = cancellationToken == null ? new CancellationToken() : cancellationToken;
        }
    }
}
