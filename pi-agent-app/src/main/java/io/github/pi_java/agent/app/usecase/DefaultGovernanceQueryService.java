package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.extension.ExtensionCapabilityStatus;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;
import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.github.pi_java.agent.app.port.mcp.McpToolStatus;
import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import io.github.pi_java.agent.client.admin.McpServerDto;
import io.github.pi_java.agent.client.admin.McpToolDto;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultGovernanceQueryService implements GovernanceQueryService {
    private final ModelProviderRegistry modelProviderRegistry;
    private final ToolRegistry toolRegistry;
    private final ExtensionGovernanceCatalog extensionGovernanceCatalog;
    private final McpGovernanceCatalog mcpGovernanceCatalog;
    private final PluginGovernanceCatalog pluginGovernanceCatalog;
    private final Optional<AgentRuntime> agentRuntime;
    private final Clock clock;

    public DefaultGovernanceQueryService(
            ModelProviderRegistry modelProviderRegistry,
            ToolRegistry toolRegistry,
            ExtensionGovernanceCatalog extensionGovernanceCatalog,
            McpGovernanceCatalog mcpGovernanceCatalog,
            PluginGovernanceCatalog pluginGovernanceCatalog,
            Optional<AgentRuntime> agentRuntime,
            Clock clock) {
        this.modelProviderRegistry = Objects.requireNonNull(modelProviderRegistry, "modelProviderRegistry must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.extensionGovernanceCatalog = Objects.requireNonNull(extensionGovernanceCatalog,
                "extensionGovernanceCatalog must not be null");
        this.mcpGovernanceCatalog = Objects.requireNonNull(mcpGovernanceCatalog,
                "mcpGovernanceCatalog must not be null");
        this.pluginGovernanceCatalog = Objects.requireNonNull(pluginGovernanceCatalog,
                "pluginGovernanceCatalog must not be null");
        this.agentRuntime = Objects.requireNonNull(agentRuntime, "agentRuntime must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public GovernanceOverviewResponse overview(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new GovernanceOverviewResponse(
                runtimeStatus(),
                providerStatus(),
                toolRegistryStatus(),
                extensionStatus(),
                mcpStatus(),
                pluginStatus(),
                policyDecisions(context),
                audits(context),
                clock.instant());
    }

    @Override
    public ExtensionGovernanceResponse extensions(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new ExtensionGovernanceResponse(extensionGovernanceCatalog.sources().stream()
                .map(DefaultGovernanceQueryService::toSourceDto)
                .toList());
    }

    @Override
    public McpGovernanceResponse mcp(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new McpGovernanceResponse(mcpGovernanceCatalog.servers().stream()
                .map(DefaultGovernanceQueryService::toMcpServerDto)
                .toList());
    }

    @Override
    public McpRefreshResponse refreshMcp(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        McpRefreshStatus status = mcpGovernanceCatalog.refresh();
        return new McpRefreshResponse(
                status.refreshed(),
                status.serverCount(),
                status.refreshedServerCount(),
                status.failedServerCount(),
                status.status(),
                status.redactedError(),
                status.metadata());
    }

    @Override
    public PluginGovernanceResponse plugins(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new PluginGovernanceResponse(pluginGovernanceCatalog.plugins().stream()
                .map(DefaultGovernanceQueryService::toPluginSourceDto)
                .toList());
    }

    @Override
    public PluginMutationResponse refreshPlugins(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return toPluginMutationResponse(pluginGovernanceCatalog.refresh());
    }

    @Override
    public PluginMutationResponse disablePlugin(RequestContext context, String pluginId, PluginMutationRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        return toPluginMutationResponse(pluginGovernanceCatalog.disable(pluginId, actor(context), request.reason()));
    }

    @Override
    public PluginMutationResponse quarantinePlugin(RequestContext context, String pluginId, PluginMutationRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        return toPluginMutationResponse(pluginGovernanceCatalog.quarantine(pluginId, actor(context), request.reason()));
    }

    @Override
    public List<PolicyDecisionSummaryDto> policyDecisions(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return List.of();
    }

    @Override
    public List<AuditSummaryDto> audits(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return List.of();
    }

    private GovernanceStatusDto runtimeStatus() {
        boolean present = agentRuntime.isPresent();
        return new GovernanceStatusDto(
                "runtime",
                present ? "HEALTHY" : "UNCONFIGURED",
                present ? "Runtime bean is available" : "Runtime bean is not configured",
                present ? 1 : 0,
                Map.of("surface", "read-only"));
    }

    private GovernanceStatusDto providerStatus() {
        int providers = modelProviderRegistry.listProviders().size();
        return new GovernanceStatusDto(
                "providers",
                providers > 0 ? "HEALTHY" : "UNCONFIGURED",
                providers > 0 ? "Model provider registry is available" : "No model providers are configured",
                providers,
                Map.of("surface", "read-only"));
    }

    private GovernanceStatusDto toolRegistryStatus() {
        int tools = toolRegistry.listTools().size();
        return new GovernanceStatusDto(
                "toolRegistry",
                tools > 0 ? "HEALTHY" : "UNCONFIGURED",
                tools > 0 ? "Governed tool registry is available" : "No governed tools are configured",
                tools,
                Map.of("surface", "read-only", "mutation", "disabled"));
    }

    private GovernanceStatusDto extensionStatus() {
        List<ExtensionSourceStatus> sources = extensionGovernanceCatalog.sources();
        int capabilityCount = sources.stream().mapToInt(ExtensionSourceStatus::capabilityCount).sum();
        long disabledSources = sources.stream().filter(source -> !source.enabled()).count();
        long incompatibleSources = sources.stream()
                .filter(source -> "INCOMPATIBLE".equals(source.compatibilityStatus()))
                .count();
        long unhealthySources = sources.stream()
                .filter(source -> "UNHEALTHY".equals(source.healthStatus()) || "FAILED".equals(source.lifecycleStatus()))
                .count();
        String status = extensionGovernanceCatalog.overallStatus();
        String message = sources.isEmpty()
                ? "No extension sources are configured"
                : "Extension governance catalog is available";
        return new GovernanceStatusDto(
                "extensions",
                status,
                message,
                sources.size(),
                Map.of(
                        "surface", "read-only",
                        "mutation", "disabled",
                        "sources", Integer.toString(sources.size()),
                        "capabilities", Integer.toString(capabilityCount),
                        "disabledSources", Long.toString(disabledSources),
                        "incompatibleSources", Long.toString(incompatibleSources),
                        "unhealthySources", Long.toString(unhealthySources)));
    }

    private GovernanceStatusDto mcpStatus() {
        List<McpServerStatus> servers = mcpGovernanceCatalog.servers();
        int toolCount = servers.stream().mapToInt(McpServerStatus::toolCount).sum();
        long disabledServers = servers.stream().filter(server -> !server.enabled()).count();
        long unhealthyServers = servers.stream()
                .filter(server -> "UNAVAILABLE".equals(server.connectionStatus())
                        || "AUTH_FAILED".equals(server.connectionStatus())
                        || "DISCOVERY_FAILED".equals(server.discoveryStatus()))
                .count();
        String message = servers.isEmpty()
                ? "No MCP servers are configured"
                : "MCP governance catalog is available";
        return new GovernanceStatusDto(
                "mcp",
                mcpGovernanceCatalog.overallStatus(),
                message,
                servers.size(),
                Map.of(
                        "surface", "read-only",
                        "mutation", "disabled",
                        "servers", Integer.toString(servers.size()),
                        "tools", Integer.toString(toolCount),
                        "disabledServers", Long.toString(disabledServers),
                        "unhealthyServers", Long.toString(unhealthyServers)));
    }

    private GovernanceStatusDto pluginStatus() {
        List<PluginSourceStatus> plugins = pluginGovernanceCatalog.plugins();
        int capabilityCount = plugins.stream().mapToInt(PluginSourceStatus::capabilityCount).sum();
        long disabledPlugins = plugins.stream()
                .filter(plugin -> "DISABLED".equals(plugin.lifecycleStatus()))
                .count();
        long quarantinedPlugins = plugins.stream()
                .filter(plugin -> "QUARANTINED".equals(plugin.lifecycleStatus()))
                .count();
        long incompatiblePlugins = plugins.stream()
                .filter(plugin -> "INCOMPATIBLE".equals(plugin.compatibilityStatus()))
                .count();
        long failedPlugins = plugins.stream()
                .filter(plugin -> "FAILED".equals(plugin.lifecycleStatus())
                        || "UNHEALTHY".equals(plugin.healthStatus()))
                .count();
        String message = plugins.isEmpty()
                ? "No plugin sources are configured"
                : "Plugin governance catalog is available";
        return new GovernanceStatusDto(
                "plugins",
                pluginGovernanceCatalog.overallStatus(),
                message,
                plugins.size(),
                Map.of(
                        "surface", "plugin-governance",
                        "mutation", "refresh,disable,quarantine",
                        "plugins", Integer.toString(plugins.size()),
                        "capabilities", Integer.toString(capabilityCount),
                        "disabledPlugins", Long.toString(disabledPlugins),
                        "quarantinedPlugins", Long.toString(quarantinedPlugins),
                        "incompatiblePlugins", Long.toString(incompatiblePlugins),
                        "failedPlugins", Long.toString(failedPlugins)));
    }

    private static ExtensionSourceDto toSourceDto(ExtensionSourceStatus source) {
        return new ExtensionSourceDto(
                source.sourceId(),
                source.name(),
                source.version(),
                source.kind(),
                source.lifecycleStatus(),
                source.enabled(),
                source.compatibilityStatus(),
                source.healthStatus(),
                source.redactedError(),
                source.capabilities().stream()
                        .map(DefaultGovernanceQueryService::toCapabilityDto)
                        .toList());
    }

    private static ExtensionCapabilityDto toCapabilityDto(ExtensionCapabilityStatus capability) {
        return new ExtensionCapabilityDto(
                capability.capabilityId(),
                capability.type(),
                capability.status(),
                capability.version(),
                capability.sourceId(),
                capability.enabled(),
                capability.compatibilityStatus(),
                capability.healthStatus(),
                capability.metadata());
    }

    private static McpServerDto toMcpServerDto(McpServerStatus server) {
        return new McpServerDto(
                server.serverId(),
                server.name(),
                server.enabled(),
                server.transport(),
                server.authSummary(),
                server.connectionStatus(),
                server.discoveryStatus(),
                server.toolCount(),
                server.lastRefreshedAt(),
                server.redactedError(),
                server.tools().stream().map(DefaultGovernanceQueryService::toMcpToolDto).toList(),
                server.metadata());
    }

    private static McpToolDto toMcpToolDto(McpToolStatus tool) {
        return new McpToolDto(
                tool.serverQualifiedToolId(),
                tool.mcpToolName(),
                tool.availabilityStatus(),
                tool.readOnly(),
                tool.destructive(),
                tool.openWorld(),
                tool.schemaSummary(),
                tool.redactedError(),
                tool.metadata());
    }

    private static PluginSourceDto toPluginSourceDto(PluginSourceStatus plugin) {
        return new PluginSourceDto(
                plugin.pluginId(),
                plugin.name(),
                plugin.version(),
                plugin.vendor(),
                plugin.sourceKind(),
                plugin.lifecycleStatus(),
                plugin.enabled(),
                plugin.healthStatus(),
                plugin.compatibilityStatus(),
                plugin.capabilityCount(),
                plugin.capabilityStatusCounts(),
                plugin.redactedError(),
                plugin.relativePathSummary(),
                plugin.reason(),
                plugin.lastUpdatedAt(),
                plugin.capabilities().stream()
                        .map(DefaultGovernanceQueryService::toPluginCapabilityDto)
                        .toList(),
                plugin.metadata());
    }

    private static PluginCapabilityDto toPluginCapabilityDto(PluginCapabilityStatus capability) {
        return new PluginCapabilityDto(
                capability.capabilityId(),
                capability.type(),
                capability.status(),
                capability.version(),
                capability.pluginId(),
                capability.enabled(),
                capability.compatibilityStatus(),
                capability.healthStatus(),
                capability.metadata());
    }

    private static PluginMutationResponse toPluginMutationResponse(PluginMutationStatus status) {
        return new PluginMutationResponse(
                status.applied(),
                status.pluginId(),
                status.operation(),
                status.previousLifecycleStatus(),
                status.resultingLifecycleStatus(),
                status.status(),
                status.redactedError(),
                status.metadata());
    }

    private static String actor(RequestContext context) {
        return context.principal().userId();
    }
}
