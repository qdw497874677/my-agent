package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.workspace.Resource;
import io.github.pi_java.agent.domain.workspace.WorkspaceGateway;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.domain.workspace.WorkspaceSession;
import io.github.pi_java.agent.domain.workspace.WorkspaceSnapshot;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FakeWorkspaceGateway implements WorkspaceGateway {
    private final Map<String, WorkspaceSession> sessions = new HashMap<>();
    private final Map<String, WorkspaceSnapshot> snapshots = new HashMap<>();
    private final Map<String, Resource> resources = new HashMap<>();
    private int sessionCounter;
    private int snapshotCounter;

    @Override
    public WorkspaceSession openSession(WorkspaceScope scope) {
        WorkspaceSession session = new WorkspaceSession("workspace-session-" + ++sessionCounter,
                scope.workspaceId(), scope.runId(), scope, Instant.EPOCH.plusSeconds(sessionCounter));
        sessions.put(session.workspaceSessionId(), session);
        return session;
    }

    @Override
    public WorkspaceSnapshot createSnapshot(String workspaceSessionId, String reason) {
        WorkspaceSession session = sessions.get(workspaceSessionId);
        if (session == null) {
            throw new IllegalArgumentException("unknown workspace session " + workspaceSessionId);
        }
        WorkspaceSnapshot snapshot = new WorkspaceSnapshot("snapshot-" + ++snapshotCounter,
                session.workspaceId(), "fingerprint-" + snapshotCounter, Map.of("reason", reason), true,
                Instant.EPOCH.plusSeconds(snapshotCounter));
        snapshots.put(snapshot.snapshotId(), snapshot);
        return snapshot;
    }

    @Override
    public void restoreSnapshot(String workspaceSessionId, String snapshotId) {
        if (!sessions.containsKey(workspaceSessionId) || !snapshots.containsKey(snapshotId)) {
            throw new IllegalArgumentException("unknown session or snapshot");
        }
    }

    @Override
    public Optional<Resource> findResource(String workspaceId, String resourceId) {
        return Optional.ofNullable(resources.get(workspaceId + ":" + resourceId));
    }

    @Override
    public void closeSession(String workspaceSessionId) {
        sessions.remove(workspaceSessionId);
    }

    public FakeWorkspaceGateway addResource(String workspaceId, Resource resource) {
        resources.put(workspaceId + ":" + resource.resourceId(), resource);
        return this;
    }
}
