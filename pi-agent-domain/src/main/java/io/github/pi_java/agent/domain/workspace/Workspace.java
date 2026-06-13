package io.github.pi_java.agent.domain.workspace;

import java.util.Set;

/**
 * First-class workspace boundary for a tenant-scoped runtime environment.
 *
 * <p>A workspace is not a host filesystem alias. It only declares logical mounts
 * over resources that infrastructure adapters may later materialize.</p>
 */
public record Workspace(String workspaceId, String tenantId, String displayName, Set<Mount> mounts) {
    public Workspace {
        mounts = Set.copyOf(mounts);
    }
}
