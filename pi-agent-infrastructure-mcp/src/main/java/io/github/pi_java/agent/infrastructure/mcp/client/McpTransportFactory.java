package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpTransportFactory implements McpClientFactory.TransportFactory {

    @Override
    public TransportHandle create(TransportRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        McpServerProperties server = request.server();
        McpClientTransport transport = switch (request.kind()) {
            case STREAMABLE_HTTP -> streamableHttp(server, request.secrets());
            case SSE -> sse(server, request.secrets());
            case STDIO -> stdio(server, request.secrets());
        };
        return new TransportHandle(request.kind(), transport);
    }

    private McpClientTransport streamableHttp(McpServerProperties server, McpSecretHeaderResolver.ResolvedTransportSecrets secrets) {
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(server.baseUrl())
                .endpoint(server.endpoint() == null ? "/mcp" : server.endpoint())
                .connectTimeout(server.timeout())
                .customizeRequest(requestBuilder -> applyHeaders(requestBuilder, secrets.headers()));
        return builder.build();
    }

    private McpClientTransport sse(McpServerProperties server, McpSecretHeaderResolver.ResolvedTransportSecrets secrets) {
        HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(server.baseUrl())
                .connectTimeout(server.timeout())
                .customizeRequest(requestBuilder -> applyHeaders(requestBuilder, secrets.headers()));
        return builder.build();
    }

    private McpClientTransport stdio(McpServerProperties server, McpSecretHeaderResolver.ResolvedTransportSecrets secrets) {
        ServerParameters parameters = ServerParameters.builder(server.command())
                .args(server.args())
                .env(secrets.env())
                .build();
        return new StdioClientTransport(parameters, McpJsonMapper.getDefault());
    }

    private static void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        headers.forEach(requestBuilder::header);
    }

    public record TransportRequest(
            String serverId,
            McpTransportKind kind,
            McpServerProperties server,
            McpSecretHeaderResolver.ResolvedTransportSecrets secrets,
            Duration timeout
    ) {
        public TransportRequest {
            serverId = requireNonBlank(serverId, "serverId");
            kind = Objects.requireNonNull(kind, "kind must not be null");
            server = Objects.requireNonNull(server, "server must not be null");
            secrets = Objects.requireNonNull(secrets, "secrets must not be null");
            timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        }
    }

    public record TransportHandle(McpTransportKind kind, Object transport) {
        public TransportHandle {
            kind = Objects.requireNonNull(kind, "kind must not be null");
            transport = Objects.requireNonNull(transport, "transport must not be null");
        }

        McpClientTransport sdkTransport() {
            return (McpClientTransport) transport;
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
