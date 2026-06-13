package io.github.pi_java.agent.domain.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    void workspaceGatewayExposesSnapshotRestoreAndResourceLookupPorts() {
        WorkspaceScope scope = new WorkspaceScope(
                "tenant-1",
                "user-1",
                "session-1",
                "run-1",
                "workspace-1",
                Set.of("resource-1"),
                Set.of("mount-1"));
        WorkspaceGateway gateway = new RecordingWorkspaceGateway(scope);

        WorkspaceSession session = gateway.openSession(scope);
        WorkspaceSnapshot snapshot = gateway.createSnapshot(session.workspaceSessionId(), "before-tool");
        gateway.restoreSnapshot(session.workspaceSessionId(), snapshot.snapshotId());

        assertThat(session.workspaceSessionId()).isEqualTo("workspace-session-1");
        assertThat(session.scope()).isEqualTo(scope);
        assertThat(snapshot.snapshotId()).isEqualTo("snapshot-1");
        assertThat(gateway.findResource("workspace-1", "resource-1"))
                .contains(new Resource("resource-1", "object", "pi://resource/1", Map.of("owner", "test")));
    }

    @Test
    void commandExecutionRequestIsACloudSafePortContractOnly() {
        CommandExecutionGateway.CommandRequest request = new CommandExecutionGateway.CommandRequest(
                "workspace-session-1",
                List.of("tool", "--inspect"),
                Map.of("PI_ENV", "test"),
                Duration.ofSeconds(10),
                "cancel-1");
        CommandExecutionGateway gateway = req -> new CommandExecutionGateway.CommandResult(
                0,
                "command accepted by fake gateway",
                "",
                false,
                false);

        CommandExecutionGateway.CommandResult result = gateway.execute(request);

        assertThat(request.workspaceSessionId()).isEqualTo("workspace-session-1");
        assertThat(request.command()).containsExactly("tool", "--inspect");
        assertThat(request.environment()).containsEntry("PI_ENV", "test");
        assertThat(request.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(request.cancellationTokenId()).isEqualTo("cancel-1");
        assertThat(result.exitCode()).isZero();
        assertThat(result.timedOut()).isFalse();
        assertThat(result.cancelled()).isFalse();
    }

    private record RecordingWorkspaceGateway(WorkspaceScope expectedScope) implements WorkspaceGateway {
        @Override
        public WorkspaceSession openSession(WorkspaceScope scope) {
            assertThat(scope).isEqualTo(expectedScope);
            return new WorkspaceSession(
                    "workspace-session-1",
                    scope.workspaceId(),
                    scope.runId(),
                    scope,
                    Instant.parse("2026-06-13T00:00:00Z"));
        }

        @Override
        public WorkspaceSnapshot createSnapshot(String workspaceSessionId, String reason) {
            assertThat(workspaceSessionId).isEqualTo("workspace-session-1");
            assertThat(reason).isEqualTo("before-tool");
            return new WorkspaceSnapshot(
                    "snapshot-1",
                    expectedScope.workspaceId(),
                    "sha256:abc123",
                    Map.of("reason", reason),
                    true,
                    Instant.parse("2026-06-13T00:00:01Z"));
        }

        @Override
        public void restoreSnapshot(String workspaceSessionId, String snapshotId) {
            assertThat(workspaceSessionId).isEqualTo("workspace-session-1");
            assertThat(snapshotId).isEqualTo("snapshot-1");
        }

        @Override
        public Optional<Resource> findResource(String workspaceId, String resourceId) {
            assertThat(workspaceId).isEqualTo(expectedScope.workspaceId());
            return Optional.of(new Resource(resourceId, "object", "pi://resource/1", Map.of("owner", "test")));
        }

        @Override
        public void closeSession(String workspaceSessionId) {
            assertThat(workspaceSessionId).isEqualTo("workspace-session-1");
        }
    }
}
