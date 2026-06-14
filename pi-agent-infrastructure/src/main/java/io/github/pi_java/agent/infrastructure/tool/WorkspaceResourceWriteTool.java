package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.infrastructure.workspace.LocalTempWorkspaceGateway;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WorkspaceResourceWriteTool {
    private final LocalTempWorkspaceGateway workspaceGateway;
    private final String workspaceSessionId;

    public WorkspaceResourceWriteTool(LocalTempWorkspaceGateway workspaceGateway, String workspaceSessionId) {
        this.workspaceGateway = Objects.requireNonNull(workspaceGateway, "workspaceGateway must not be null");
        this.workspaceSessionId = Objects.requireNonNull(workspaceSessionId, "workspaceSessionId must not be null");
    }

    public InMemoryToolRegistry.ToolRegistration registration() {
        ToolDescriptor descriptor = new ToolDescriptor(
                "builtin.workspace.write",
                "Workspace Resource Write",
                "Writes or appends a workspace-scoped resource after gateway preview/approval.",
                schema(),
                Optional.empty(),
                ReadOnlyInfoTool.provenance("workspace-resource-write"),
                "1.0.0",
                Set.of("tool:workspace:write"),
                ToolRiskLevel.MEDIUM,
                ToolSideEffect.WORKSPACE_WRITE,
                Duration.ofSeconds(5),
                Map.of("category", "workspace-resource-write", "previewRequired", true, "approvalRecommended", true)
        );
        return new InMemoryToolRegistry.ToolRegistration(descriptor, (request, cancellationToken) -> {
            String path = Objects.toString(request.arguments().get("path"), "");
            String content = Objects.toString(request.arguments().get("content"), "");
            boolean append = Boolean.TRUE.equals(request.arguments().get("append"));
            try {
                workspaceGateway.writeText(workspaceSessionId, path, content, append);
                Map<String, Object> output = Map.of("path", path, "bytes", content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, "append", append);
                return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                        "workspace resource written", Optional.empty(), Map.of("path", path, "append", append), output,
                        Set.of(), Optional.empty(), Duration.ZERO, Optional.of(output));
            } catch (RuntimeException e) {
                return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.FAILED,
                        "workspace write failed: " + e.getClass().getSimpleName(), Optional.of("WORKSPACE_WRITE_FAILED"),
                        Map.of("path", path), Map.of(), Set.of(), Optional.empty(), Duration.ZERO, Optional.empty());
            }
        });
    }

    private static ToolSchema schema() {
        return new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", java.util.List.of("path", "content"),
                "properties", Map.of(
                        "path", Map.of("type", "string", "minLength", 1),
                        "content", Map.of("type", "string"),
                        "append", Map.of("type", "boolean")
                )
        ), Set.of("content"), 16384);
    }
}
