package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator;
import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.app.usecase.DefaultToolExecutionGateway;
import io.github.pi_java.agent.app.usecase.DefaultToolRegistryQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;
import io.github.pi_java.agent.infrastructure.tool.BuiltinToolCatalog;
import io.github.pi_java.agent.infrastructure.tool.DefaultToolPayloadLimiter;
import io.github.pi_java.agent.infrastructure.tool.DefaultToolPolicyEvaluator;
import io.github.pi_java.agent.infrastructure.tool.DefaultToolPreviewGenerator;
import io.github.pi_java.agent.infrastructure.tool.DefaultToolRedactor;
import io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry;
import io.github.pi_java.agent.infrastructure.tool.NetworkntToolArgumentValidator;
import io.github.pi_java.agent.infrastructure.tool.ReadOnlyInfoTool;
import io.github.pi_java.agent.infrastructure.tool.WorkspaceCommandTool;
import io.github.pi_java.agent.infrastructure.tool.WorkspaceResourceWriteTool;
import io.github.pi_java.agent.infrastructure.workspace.AllowlistedCommandExecutionGateway;
import io.github.pi_java.agent.infrastructure.workspace.LocalTempWorkspaceGateway;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpToolRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class ToolGovernanceBeanConfiguration {

    static final String BUILTIN_WORKSPACE_SESSION_ID = "cloud-builtins";

    @Bean
    @ConditionalOnMissingBean
    LocalTempWorkspaceGateway localTempWorkspaceGateway() {
        try {
            return new LocalTempWorkspaceGateway(Files.createTempDirectory("pi-java-cloud-workspaces-"));
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to create local temp workspace root", ex);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    CommandExecutionGateway commandExecutionGateway(
            LocalTempWorkspaceGateway workspaceGateway,
            @Value("${pi.tools.builtins.command-allowlist:pwd,ls,cat}") String allowlist) {
        return new AllowlistedCommandExecutionGateway(workspaceGateway, parseAllowlist(allowlist), Duration.ofSeconds(5), 4096);
    }

    @Bean
    @ConditionalOnMissingBean
    BuiltinToolCatalog builtinToolCatalog(LocalTempWorkspaceGateway workspaceGateway, CommandExecutionGateway commandExecutionGateway) {
        workspaceGateway.openSession(new io.github.pi_java.agent.domain.workspace.WorkspaceScope(
                "system", "system", BUILTIN_WORKSPACE_SESSION_ID, BUILTIN_WORKSPACE_SESSION_ID,
                "workspace-builtins", Set.of(), Set.of()));
        return new BuiltinToolCatalog(
                new ReadOnlyInfoTool(Map.of("platform", "pi-java", "toolGovernance", "enabled")),
                new WorkspaceResourceWriteTool(workspaceGateway, BUILTIN_WORKSPACE_SESSION_ID),
                new WorkspaceCommandTool(commandExecutionGateway, BUILTIN_WORKSPACE_SESSION_ID));
    }

    @Bean
    @ConditionalOnMissingBean(name = "toolRegistry")
    @Primary
    ToolRegistry toolRegistry(BuiltinToolCatalog builtinToolCatalog,
                                Optional<DefaultExtensionContributionRegistry> extensionContributions,
                                Optional<McpToolRegistry> mcpToolRegistry,
                                @Qualifier("pluginToolRegistry") Optional<ToolRegistry> pluginToolRegistry) {
        ToolRegistry builtins = builtinToolCatalog.registry();
        java.util.ArrayList<ToolRegistry> registries = new java.util.ArrayList<>();
        registries.add(builtins);
        extensionContributions.ifPresent(contributions -> registries.add(new ExtensionToolRegistry(contributions)));
        mcpToolRegistry.ifPresent(registries::add);
        pluginToolRegistry.ifPresent(registries::add);
        return registries.size() == 1 ? builtins : new CompositeToolRegistry(registries);
    }

    @Bean
    @ConditionalOnMissingBean
    ToolRegistryQueryService toolRegistryQueryService(ToolRegistry toolRegistry) {
        return new DefaultToolRegistryQueryService(toolRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    ToolArgumentValidator toolArgumentValidator() {
        return new NetworkntToolArgumentValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    ToolPolicyEvaluator toolPolicyEvaluator() {
        return new DefaultToolPolicyEvaluator(new AgentDefinition(
                new AgentId("cloud-general-agent"),
                "Cloud General Agent",
                "Default Cloud Server agent policy for governed built-in tools.",
                "openai-compatible:gpt-4.1-mini",
                Set.of("tool:read", "tool:workspace:write", "tool:workspace:command"),
                Set.of("default-tool-policy"),
                new RuntimeLimits(Duration.ofSeconds(30), 8, 8),
                Set.of(InteractionMode.CHAT, InteractionMode.TASK),
                "default-workspace-policy",
                "default-output-policy"));
    }

    @Bean
    @ConditionalOnMissingBean
    ToolRedactor toolRedactor() {
        return new DefaultToolRedactor();
    }

    @Bean
    @ConditionalOnMissingBean
    ToolPayloadLimiter toolPayloadLimiter() {
        return new DefaultToolPayloadLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    ToolPreviewGenerator toolPreviewGenerator() {
        return new DefaultToolPreviewGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    ToolExecutionGateway toolExecutionGateway(
            ToolRegistry toolRegistry,
            ToolArgumentValidator argumentValidator,
            ToolPolicyEvaluator policyEvaluator,
            ToolRedactor redactor,
            ToolPayloadLimiter payloadLimiter,
            ToolPreviewGenerator previewGenerator,
            AuditRepository auditRepository,
            EventSink eventSink,
            Clock clock) {
        return new DefaultToolExecutionGateway(toolRegistry, argumentValidator, policyEvaluator, redactor, payloadLimiter,
                previewGenerator, auditRepository, eventSink, clock);
    }

    private static Set<String> parseAllowlist(String allowlist) {
        return java.util.Arrays.stream(allowlist.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private record CompositeToolRegistry(List<ToolRegistry> registries) implements ToolRegistry {
        private CompositeToolRegistry {
            registries = List.copyOf(registries);
        }

        @Override
        public List<io.github.pi_java.agent.domain.tool.ToolDescriptor> listTools() {
            Map<String, io.github.pi_java.agent.domain.tool.ToolDescriptor> tools = new LinkedHashMap<>();
            for (ToolRegistry registry : registries) {
                for (io.github.pi_java.agent.domain.tool.ToolDescriptor descriptor : registry.listTools()) {
                    tools.putIfAbsent(descriptor.id(), descriptor);
                }
            }
            return List.copyOf(tools.values());
        }

        @Override
        public Optional<ToolResolution> resolve(String toolId) {
            return registries.stream()
                    .map(registry -> registry.resolve(toolId))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
    }
}
