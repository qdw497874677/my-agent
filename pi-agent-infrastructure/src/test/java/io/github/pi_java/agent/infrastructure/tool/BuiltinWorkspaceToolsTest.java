package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.domain.workspace.WorkspaceSession;
import io.github.pi_java.agent.infrastructure.workspace.AllowlistedCommandExecutionGateway;
import io.github.pi_java.agent.infrastructure.workspace.LocalTempWorkspaceGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinWorkspaceToolsTest {

    @TempDir
    Path tempDir;

    @Test
    void catalogExposesMinimumThreeBuiltInCategories() {
        BuiltinToolCatalog catalog = catalog();

        assertThat(catalog.registrations()).hasSize(3);
        assertThat(catalog.descriptors().stream().map(ToolDescriptor::id))
                .containsExactlyInAnyOrder("builtin.info", "builtin.workspace.write", "builtin.workspace.command");
        assertThat(catalog.descriptors().stream().map(descriptor -> descriptor.metadata().get("category")))
                .containsExactlyInAnyOrder("read-only-info", "workspace-resource-write", "workspace-command");
    }

    @Test
    void writeAndCommandDescriptorsAreSideEffectfulAndApprovalFriendly() {
        BuiltinToolCatalog catalog = catalog();

        ToolDescriptor write = descriptor(catalog, "builtin.workspace.write");
        ToolDescriptor command = descriptor(catalog, "builtin.workspace.command");

        assertThat(write.sideEffect()).isEqualTo(ToolSideEffect.WORKSPACE_WRITE);
        assertThat(command.sideEffect()).isEqualTo(ToolSideEffect.WORKSPACE_WRITE);
        assertThat(write.metadata()).containsEntry("previewRequired", true).containsEntry("approvalRecommended", true);
        assertThat(command.metadata()).containsEntry("previewRequired", true).containsEntry("approvalRecommended", true);
    }

    @Test
    void toolsUseWorkspaceScopedResourcesAndInjectedValues() {
        LocalTempWorkspaceGateway workspace = new LocalTempWorkspaceGateway(tempDir);
        WorkspaceSession session = workspace.openSession(scope());
        BuiltinToolCatalog catalog = catalog(workspace, session.workspaceSessionId());
        CancellationToken token = new CancellationToken();

        ToolExecutionResult info = binding(catalog, "builtin.info").execute(request("builtin.info", Map.of()), token);
        assertThat(info.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(info.rawOutput()).contains(Map.of("serviceName", "pi-java-test", "environment", "test"));

        ToolExecutionResult write = binding(catalog, "builtin.workspace.write").execute(request("builtin.workspace.write", Map.of(
                "path", "artifacts/note.txt",
                "content", "hello workspace",
                "append", false
        )), token);
        assertThat(write.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(workspace.readText(session.workspaceSessionId(), "artifacts/note.txt")).isEqualTo("hello workspace");

        ToolExecutionResult command = binding(catalog, "builtin.workspace.command").execute(request("builtin.workspace.command", Map.of(
                "command", java.util.List.of("printf", "ok")
        )), token);
        assertThat(command.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(command.rawOutput().orElseThrow()).containsEntry("outputSummary", "ok");

        ToolExecutionResult escapedWrite = binding(catalog, "builtin.workspace.write").execute(request("builtin.workspace.write", Map.of(
                "path", "../escape.txt",
                "content", "bad"
        )), token);
        assertThat(escapedWrite.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(escapedWrite.summary()).contains("workspace write failed");
    }

    private BuiltinToolCatalog catalog() {
        LocalTempWorkspaceGateway workspace = new LocalTempWorkspaceGateway(tempDir);
        WorkspaceSession session = workspace.openSession(scope());
        return catalog(workspace, session.workspaceSessionId());
    }

    private BuiltinToolCatalog catalog(LocalTempWorkspaceGateway workspace, String workspaceSessionId) {
        return new BuiltinToolCatalog(
                new ReadOnlyInfoTool(Map.of("serviceName", "pi-java-test", "environment", "test")),
                new WorkspaceResourceWriteTool(workspace, workspaceSessionId),
                new WorkspaceCommandTool(new AllowlistedCommandExecutionGateway(workspace, Set.of("printf"), Duration.ofSeconds(2), 256), workspaceSessionId)
        );
    }

    private ToolDescriptor descriptor(BuiltinToolCatalog catalog, String id) {
        return catalog.descriptors().stream().filter(descriptor -> descriptor.id().equals(id)).findFirst().orElseThrow();
    }

    private io.github.pi_java.agent.app.port.tool.ToolExecutorBinding binding(BuiltinToolCatalog catalog, String id) {
        return catalog.registrations().stream()
                .filter(registration -> registration.descriptor().id().equals(id))
                .findFirst()
                .orElseThrow()
                .executor();
    }

    private ToolExecutionRequest request(String toolId, Map<String, Object> arguments) {
        return new ToolExecutionRequest("call-" + toolId, new RunId("run-1"), new StepId("step-1"), toolId,
                "1.0.0", arguments, Instant.parse("2026-06-14T00:00:00Z"));
    }

    private WorkspaceScope scope() {
        return new WorkspaceScope("tenant", "user", "session", "run", "workspace", Set.of(), Set.of());
    }
}
