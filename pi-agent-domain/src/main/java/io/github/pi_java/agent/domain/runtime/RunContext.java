package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;

import java.time.Instant;
import java.util.Objects;

public record RunContext(
        AgentDefinition agentDefinition,
        RunInput input,
        SessionContext sessionContext,
        WorkspaceScope workspaceScope,
        RuntimeLimits limits,
        CancellationToken cancellationToken,
        String traceId,
        Instant startedAt
) {
    public RunContext {
        Objects.requireNonNull(agentDefinition, "agentDefinition must not be null");
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        Objects.requireNonNull(workspaceScope, "workspaceScope must not be null");
        Objects.requireNonNull(limits, "limits must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        Objects.requireNonNull(startedAt, "startedAt must not be null");
    }
}
