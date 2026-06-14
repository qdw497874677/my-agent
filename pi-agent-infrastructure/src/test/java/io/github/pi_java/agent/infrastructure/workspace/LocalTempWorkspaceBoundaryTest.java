package io.github.pi_java.agent.infrastructure.workspace;

import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.domain.workspace.WorkspaceSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalTempWorkspaceBoundaryTest {

    @TempDir
    Path tempDir;

    @Test
    void workspaceRejectsTraversalAndAbsolutePathEscapes() throws Exception {
        LocalTempWorkspaceGateway workspace = new LocalTempWorkspaceGateway(tempDir);
        WorkspaceSession session = workspace.openSession(scope("run-a"));

        workspace.writeText(session.workspaceSessionId(), "artifacts/result.txt", "safe", false);

        assertThat(workspace.readText(session.workspaceSessionId(), "artifacts/result.txt")).isEqualTo("safe");
        assertThatThrownBy(() -> workspace.writeText(session.workspaceSessionId(), "../escape.txt", "bad", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspace boundary");
        assertThatThrownBy(() -> workspace.readText(session.workspaceSessionId(), tempDir.resolve("outside.txt").toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspace boundary");
        assertThat(Files.exists(tempDir.resolve("escape.txt"))).isFalse();
    }

    @Test
    void commandGatewayRejectsNonAllowlistedCommandsAndRunsSafeCommandsInWorkspaceRoot() {
        LocalTempWorkspaceGateway workspace = new LocalTempWorkspaceGateway(tempDir);
        WorkspaceSession session = workspace.openSession(scope("run-b"));
        AllowlistedCommandExecutionGateway commands = new AllowlistedCommandExecutionGateway(
                workspace,
                Set.of("pwd", "printf"),
                Duration.ofSeconds(2),
                512
        );

        CommandExecutionGateway.CommandResult denied = commands.execute(new CommandExecutionGateway.CommandRequest(
                session.workspaceSessionId(), List.of("sh", "-c", "pwd"), Map.of(), Duration.ofSeconds(1), null));
        assertThat(denied.exitCode()).isEqualTo(126);
        assertThat(denied.errorSummary()).contains("not allowlisted");

        CommandExecutionGateway.CommandResult pwd = commands.execute(new CommandExecutionGateway.CommandRequest(
                session.workspaceSessionId(), List.of("pwd"), Map.of(), Duration.ofSeconds(1), null));
        assertThat(pwd.exitCode()).isZero();
        assertThat(pwd.outputSummary()).contains(workspace.rootFor(session.workspaceSessionId()).toString());
    }

    @Test
    void commandTimeoutProducesBoundedSummaries() {
        LocalTempWorkspaceGateway workspace = new LocalTempWorkspaceGateway(tempDir);
        WorkspaceSession session = workspace.openSession(scope("run-c"));
        AllowlistedCommandExecutionGateway commands = new AllowlistedCommandExecutionGateway(
                workspace,
                Set.of("sleep"),
                Duration.ofMillis(100),
                32
        );

        CommandExecutionGateway.CommandResult result = commands.execute(new CommandExecutionGateway.CommandRequest(
                session.workspaceSessionId(), List.of("sleep", "2"), Map.of("SECRET", "ignored"), Duration.ofMillis(100), null));

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.outputSummary().length()).isLessThanOrEqualTo(80);
        assertThat(result.errorSummary()).contains("timed out");
    }

    private static WorkspaceScope scope(String runId) {
        return new WorkspaceScope("tenant", "user", "session", runId, "workspace-" + runId, Set.of(), Set.of());
    }
}
