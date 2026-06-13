package io.github.pi_java.agent.domain.workspace;

import java.util.Set;

/**
 * Tenant/user/session/run context plus allowed logical resource boundaries.
 */
public record WorkspaceScope(
        String tenantId,
        String userId,
        String sessionId,
        String runId,
        String workspaceId,
        Set<String> allowedResourceIds,
        Set<String> allowedMountIds) {
    public WorkspaceScope {
        allowedResourceIds = Set.copyOf(allowedResourceIds);
        allowedMountIds = Set.copyOf(allowedMountIds);
    }
}
