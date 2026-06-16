package io.github.pi_java.agent.infrastructure.mcp.invocation;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientFactory;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientHandle;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class McpToolExecutorBinding implements ToolExecutorBinding {
    private final McpServerProperties server;
    private final String mcpToolName;
    private final InvocationClientFactory clientFactory;
    private final McpToolResultMapper resultMapper;
    private final McpInvocationErrorMapper errorMapper;

    public McpToolExecutorBinding(McpServerProperties server, String mcpToolName, McpClientFactory clientFactory) {
        this(server, mcpToolName, new HandleInvocationClientFactory(clientFactory));
    }

    public McpToolExecutorBinding(McpServerProperties server, String mcpToolName, InvocationClientFactory clientFactory) {
        this(server, mcpToolName, clientFactory, new McpToolResultMapper(), new McpInvocationErrorMapper());
    }

    McpToolExecutorBinding(McpServerProperties server, String mcpToolName, InvocationClientFactory clientFactory,
                           McpToolResultMapper resultMapper, McpInvocationErrorMapper errorMapper) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.mcpToolName = requireNonBlank(mcpToolName, "mcpToolName");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory must not be null");
        this.resultMapper = Objects.requireNonNull(resultMapper, "resultMapper must not be null");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper must not be null");
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken cancellationToken) {
        Objects.requireNonNull(request, "request must not be null");
        CancellationToken token = cancellationToken == null ? new CancellationToken() : cancellationToken;
        Instant startedAt = Instant.now();
        if (token.isCancellationRequested()) {
            return errorMapper.cancelled(request, server.id(), mcpToolName, elapsedSince(startedAt));
        }
        try (InvocationClient client = clientFactory.open(server)) {
            if (token.isCancellationRequested()) {
                return errorMapper.cancelled(request, server.id(), mcpToolName, elapsedSince(startedAt));
            }
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(mcpToolName, request.arguments()));
            return resultMapper.map(request, server.id(), mcpToolName, result, elapsedSince(startedAt));
        } catch (RuntimeException ex) {
            return errorMapper.failure(request, server.id(), mcpToolName, ex, elapsedSince(startedAt));
        }
    }

    private static Duration elapsedSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now());
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public interface InvocationClientFactory {
        InvocationClient open(McpServerProperties server);
    }

    public interface InvocationClient extends AutoCloseable {
        McpSchema.CallToolResult callTool(McpSchema.CallToolRequest request);

        @Override
        void close();
    }

    private record HandleInvocationClientFactory(McpClientFactory clientFactory) implements InvocationClientFactory {
        private HandleInvocationClientFactory {
            Objects.requireNonNull(clientFactory, "clientFactory must not be null");
        }

        @Override
        public InvocationClient open(McpServerProperties server) {
            return new HandleInvocationClient(clientFactory.create(server));
        }
    }

    private record HandleInvocationClient(McpClientHandle handle) implements InvocationClient {
        private HandleInvocationClient {
            Objects.requireNonNull(handle, "handle must not be null");
        }

        @Override
        public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest request) {
            return handle.callTool(request);
        }

        @Override
        public void close() {
            handle.close();
        }
    }
}
