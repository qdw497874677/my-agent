package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class McpToolRegistry implements ToolRegistry {
    private final McpServerRegistry serverRegistry;
    private final McpToolDescriptorMapper mapper;
    private final ToolExecutorBinding deferredExecutor = new DeferredMcpToolExecutorBinding();

    public McpToolRegistry(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper) {
        this.serverRegistry = Objects.requireNonNull(serverRegistry, "serverRegistry must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return serverRegistry.snapshots().stream()
                .filter(snapshot -> "DISCOVERED".equals(snapshot.discoveryStatus()))
                .flatMap(snapshot -> snapshot.tools().stream()
                        .filter(McpServerRegistry.DiscoveredTool::available)
                        .map(tool -> mapper.toDescriptor(snapshot.server(), tool)))
                .toList();
    }

    @Override
    public Optional<ToolResolution> resolve(String toolId) {
        if (toolId == null || toolId.isBlank()) {
            return Optional.empty();
        }
        return listTools().stream()
                .filter(descriptor -> descriptor.id().equals(toolId))
                .findFirst()
                .map(descriptor -> new ToolResolution(descriptor, deferredExecutor));
    }

    private static final class DeferredMcpToolExecutorBinding implements ToolExecutorBinding {
        @Override
        public ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken cancellationToken) {
            String toolCallId = request == null ? "mcp-deferred" : request.toolCallId();
            String toolId = request == null ? "mcp.deferred" : request.toolId();
            return new ToolExecutionResult(toolCallId, toolId, ToolExecutionStatus.FAILED,
                    "MCP remote execution is deferred to the governed execution adapter in Plan 07-05.",
                    "MCP_EXECUTION_DEFERRED", Map.of("source", "mcp"), Map.of(), Set.of(), null, Duration.ZERO);
        }
    }
}
