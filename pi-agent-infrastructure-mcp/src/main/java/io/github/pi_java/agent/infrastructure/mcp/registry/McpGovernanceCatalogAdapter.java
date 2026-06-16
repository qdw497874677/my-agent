package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.github.pi_java.agent.app.port.mcp.McpToolStatus;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpGovernanceCatalogAdapter implements McpGovernanceCatalog {
    private final McpServerRegistry serverRegistry;
    private final McpToolDescriptorMapper mapper;

    public McpGovernanceCatalogAdapter(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper) {
        this.serverRegistry = Objects.requireNonNull(serverRegistry, "serverRegistry must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<McpServerStatus> servers() {
        return serverRegistry.snapshots().stream()
                .map(this::toServerStatus)
                .toList();
    }

    @Override
    public McpRefreshStatus refresh() {
        McpDiscoveryResult result = serverRegistry.refresh();
        return new McpRefreshStatus(true, result.refreshedServers(), result.failedServers(), result.configuredServers(),
                result.success() ? "SUCCESS" : "PARTIAL_FAILURE", result.redactedError(), Map.of("surface", "read-only-refresh"));
    }

    private McpServerStatus toServerStatus(McpServerRegistry.ServerSnapshot snapshot) {
        List<McpToolStatus> tools = snapshot.tools().stream()
                .filter(McpServerRegistry.DiscoveredTool::available)
                .map(tool -> toToolStatus(snapshot, tool))
                .toList();
        return new McpServerStatus(snapshot.server().id(), snapshot.server().displayName(), snapshot.server().enabled(),
                snapshot.server().transport().name(), authSummary(snapshot.server()), snapshot.connectionStatus(),
                snapshot.discoveryStatus(), tools.size(), snapshot.lastRefreshedAt(), snapshot.redactedError(), tools,
                Map.of("metadata.count", Integer.toString(snapshot.server().metadata().size())));
    }

    private McpToolStatus toToolStatus(McpServerRegistry.ServerSnapshot snapshot, McpServerRegistry.DiscoveredTool tool) {
        ToolDescriptor descriptor = mapper.toDescriptor(snapshot.server(), tool);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("riskLevel", descriptor.riskLevel().name());
        metadata.put("sideEffect", descriptor.sideEffect().name());
        metadata.put("scopeCount", Integer.toString(descriptor.scopes().size()));
        return new McpToolStatus(descriptor.id(), tool.name(), tool.available() ? "AVAILABLE" : "UNAVAILABLE",
                Boolean.TRUE.equals(descriptor.metadata().get("mcp.readOnlyHint")),
                Boolean.TRUE.equals(descriptor.metadata().get("mcp.destructiveHint")),
                Boolean.TRUE.equals(descriptor.metadata().get("mcp.openWorldHint")),
                schemaSummary(descriptor), tool.redactedError(), metadata);
    }

    private static String authSummary(io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties server) {
        return server.auth().authKind() + ":headers=" + server.auth().customHeaderSecretRefs().size()
                + ":env=" + server.envSecretRefs().size();
    }

    private static String schemaSummary(ToolDescriptor descriptor) {
        return descriptor.inputSchema().dialect() + ":keys=" + descriptor.inputSchema().document().keySet().size();
    }
}
