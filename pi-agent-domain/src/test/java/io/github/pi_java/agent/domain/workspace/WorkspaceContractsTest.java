package io.github.pi_java.agent.domain.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkspaceContractsTest {

    @Test
    void workspaceScopeCarriesRunWorkspaceAndAllowedBoundaryIds() {
        WorkspaceScope scope = new WorkspaceScope(
                "tenant-1",
                "user-1",
                "session-1",
                "run-1",
                "workspace-1",
                Set.of("resource-1", "resource-2"),
                Set.of("mount-1"));

        assertThat(scope.tenantId()).isEqualTo("tenant-1");
        assertThat(scope.userId()).isEqualTo("user-1");
        assertThat(scope.sessionId()).isEqualTo("session-1");
        assertThat(scope.runId()).isEqualTo("run-1");
        assertThat(scope.workspaceId()).isEqualTo("workspace-1");
        assertThat(scope.allowedResourceIds()).containsExactlyInAnyOrder("resource-1", "resource-2");
        assertThat(scope.allowedMountIds()).containsExactly("mount-1");
    }

    @Test
    void workspaceSnapshotCarriesFingerprintDriftMetadataAndReplaySafety() {
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        WorkspaceSnapshot snapshot = new WorkspaceSnapshot(
                "snapshot-1",
                "workspace-1",
                "sha256:abc123",
                Map.of("resource-1", "unchanged", "mount-1", "clean"),
                true,
                createdAt);

        assertThat(snapshot.snapshotId()).isEqualTo("snapshot-1");
        assertThat(snapshot.workspaceId()).isEqualTo("workspace-1");
        assertThat(snapshot.fingerprint()).isEqualTo("sha256:abc123");
        assertThat(snapshot.driftMetadata()).containsEntry("resource-1", "unchanged");
        assertThat(snapshot.replaySafe()).isTrue();
        assertThat(snapshot.createdAt()).isEqualTo(createdAt);
    }

}
