package io.github.pi_java.agent.domain.workspace;

import java.time.Instant;

/**
 * Run-scoped workspace session opened inside a constrained workspace scope.
 */
public record WorkspaceSession(
        String workspaceSessionId,
        String workspaceId,
        String runId,
        WorkspaceScope scope,
        Instant openedAt) {}
