package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientFactory;
import io.github.pi_java.agent.infrastructure.mcp.client.McpSecretHeaderResolver;
import io.github.pi_java.agent.infrastructure.mcp.invocation.McpToolExecutorBinding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class McpToolRegistry implements ToolRegistry {
    private final McpServerRegistry serverRegistry;
    private final McpToolDescriptorMapper mapper;
    private final ToolBindingFactory bindingFactory;

    public McpToolRegistry(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper, McpSecretHeaderResolver secretResolver) {
        this(serverRegistry, mapper, new McpClientFactory(secretResolver));
    }

    McpToolRegistry(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper,
                    McpToolExecutorBinding.InvocationClientFactory invocationClientFactory) {
        this(serverRegistry, mapper, new McpToolExecutorBindingFactory(invocationClientFactory));
    }

    public McpToolRegistry(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper, McpClientFactory clientFactory) {
        this(serverRegistry, mapper, new McpClientToolBindingFactory(clientFactory));
    }

    private McpToolRegistry(McpServerRegistry serverRegistry, McpToolDescriptorMapper mapper, ToolBindingFactory bindingFactory) {
        this.serverRegistry = Objects.requireNonNull(serverRegistry, "serverRegistry must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.bindingFactory = Objects.requireNonNull(bindingFactory, "bindingFactory must not be null");
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
        return serverRegistry.snapshots().stream()
                .filter(snapshot -> "DISCOVERED".equals(snapshot.discoveryStatus()))
                .flatMap(snapshot -> snapshot.tools().stream()
                        .filter(McpServerRegistry.DiscoveredTool::available)
                        .filter(tool -> mapper.toDescriptor(snapshot.server(), tool).id().equals(toolId))
                        .map(tool -> new ToolResolution(mapper.toDescriptor(snapshot.server(), tool),
                                bindingFactory.create(snapshot.server(), tool.name()))))
                .findFirst();
    }

    private interface ToolBindingFactory {
        McpToolExecutorBinding create(io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties server, String toolName);
    }

    private record McpClientToolBindingFactory(McpClientFactory clientFactory) implements ToolBindingFactory {
        private McpClientToolBindingFactory {
            Objects.requireNonNull(clientFactory, "clientFactory must not be null");
        }

        @Override
        public McpToolExecutorBinding create(io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties server, String toolName) {
            return new McpToolExecutorBinding(server, toolName, clientFactory);
        }
    }

    private record McpToolExecutorBindingFactory(McpToolExecutorBinding.InvocationClientFactory invocationClientFactory)
            implements ToolBindingFactory {
        private McpToolExecutorBindingFactory {
            Objects.requireNonNull(invocationClientFactory, "invocationClientFactory must not be null");
        }

        @Override
        public McpToolExecutorBinding create(io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties server, String toolName) {
            return new McpToolExecutorBinding(server, toolName, invocationClientFactory);
        }
    }
}
