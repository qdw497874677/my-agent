package io.github.pi_java.agent.domain.workspace;

import java.util.Optional;

/**
 * Domain port for workspace lifecycle, resource lookup, and snapshot operations.
 *
 * <p>Implementations belong to infrastructure/testkit layers. Domain callers do
 * not assume any host filesystem layout or storage mechanism.</p>
 */
public interface WorkspaceGateway {
    WorkspaceSession openSession(WorkspaceScope scope);

    WorkspaceSnapshot createSnapshot(String workspaceSessionId, String reason);

    void restoreSnapshot(String workspaceSessionId, String snapshotId);

    Optional<Resource> findResource(String workspaceId, String resourceId);

    void closeSession(String workspaceSessionId);
}
