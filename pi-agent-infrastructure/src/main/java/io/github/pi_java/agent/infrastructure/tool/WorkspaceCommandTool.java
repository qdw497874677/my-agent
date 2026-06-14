package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WorkspaceCommandTool {
    private final CommandExecutionGateway commandExecutionGateway;
    private final String workspaceSessionId;

    public WorkspaceCommandTool(CommandExecutionGateway commandExecutionGateway, String workspaceSessionId) {
        this.commandExecutionGateway = Objects.requireNonNull(commandExecutionGateway, "commandExecutionGateway must not be null");
        this.workspaceSessionId = Objects.requireNonNull(workspaceSessionId, "workspaceSessionId must not be null");
    }

    public InMemoryToolRegistry.ToolRegistration registration() {
        ToolDescriptor descriptor = new ToolDescriptor(
                "builtin.workspace.command",
                "Workspace Allowlisted Command",
                "Executes an allowlisted command inside a workspace boundary after gateway preview/approval.",
                schema(),
                Optional.empty(),
                ReadOnlyInfoTool.provenance("workspace-command"),
                "1.0.0",
                Set.of("tool:workspace:command"),
                ToolRiskLevel.MEDIUM,
                ToolSideEffect.WORKSPACE_WRITE,
                Duration.ofSeconds(5),
                Map.of("category", "workspace-command", "previewRequired", true, "approvalRecommended", true)
        );
        return new InMemoryToolRegistry.ToolRegistration(descriptor, (request, cancellationToken) -> {
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) request.arguments().getOrDefault("command", List.of());
            CommandExecutionGateway.CommandResult result = commandExecutionGateway.execute(new CommandExecutionGateway.CommandRequest(
                    workspaceSessionId, command, Map.of(), descriptor.defaultTimeout(), request.toolCallId()));
            ToolExecutionStatus status = result.cancelled()
                    ? ToolExecutionStatus.CANCELLED
                    : result.timedOut() ? ToolExecutionStatus.TIMED_OUT : result.exitCode() == 0 ? ToolExecutionStatus.SUCCESS : ToolExecutionStatus.FAILED;
            Map<String, Object> output = Map.of(
                    "exitCode", result.exitCode(),
                    "outputSummary", result.outputSummary(),
                    "errorSummary", result.errorSummary(),
                    "timedOut", result.timedOut(),
                    "cancelled", result.cancelled()
            );
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), status,
                    status == ToolExecutionStatus.SUCCESS ? "workspace command completed" : "workspace command did not complete successfully",
                    status == ToolExecutionStatus.SUCCESS ? Optional.empty() : Optional.of("COMMAND_FAILED"),
                    Map.of("command", command.isEmpty() ? "" : command.getFirst()), output, Set.of(), Optional.empty(), Duration.ZERO, Optional.of(output));
        });
    }

    private static ToolSchema schema() {
        return new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", java.util.List.of("command"),
                "properties", Map.of("command", Map.of("type", "array", "items", Map.of("type", "string"), "minItems", 1))
        ), Set.of(), 8192);
    }
}
