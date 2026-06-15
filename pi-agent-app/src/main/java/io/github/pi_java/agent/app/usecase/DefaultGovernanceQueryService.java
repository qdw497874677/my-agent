package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.extension.ExtensionCapabilityStatus;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;
import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
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
    private final Optional<AgentRuntime> agentRuntime;
    private final Clock clock;

    public DefaultGovernanceQueryService(
            ModelProviderRegistry modelProviderRegistry,
            ToolRegistry toolRegistry,
            ExtensionGovernanceCatalog extensionGovernanceCatalog,
            Optional<AgentRuntime> agentRuntime,
            Clock clock) {
        this.modelProviderRegistry = Objects.requireNonNull(modelProviderRegistry, "modelProviderRegistry must not be null");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.extensionGovernanceCatalog = Objects.requireNonNull(extensionGovernanceCatalog,
                "extensionGovernanceCatalog must not be null");
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
                futureStatus("mcp", "UNCONFIGURED", "Remote MCP governance arrives in Phase 7"),
                futureStatus("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8"),
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

    private static GovernanceStatusDto futureStatus(String area, String status, String message) {
        return new GovernanceStatusDto(area, status, message, 0, Map.of("surface", "placeholder", "mutation", "disabled"));
    }
}
