package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.infrastructure.mcp.config.McpSafetyValidator;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Objects;

public final class McpClientFactory {
    private final TransportFactory transportFactory;
    private final ClientBuilder clientBuilder;
    private final McpSecretHeaderResolver secretResolver;
    private final McpClientErrorSanitizer errorSanitizer;

    public McpClientFactory(McpSecretHeaderResolver secretResolver) {
        this(new McpTransportFactory(), new SdkClientBuilder(), secretResolver, McpClientErrorSanitizer.defaults());
    }

    McpClientFactory(TransportFactory transportFactory, ClientBuilder clientBuilder, McpSecretHeaderResolver secretResolver, McpClientErrorSanitizer errorSanitizer) {
        this.transportFactory = Objects.requireNonNull(transportFactory, "transportFactory must not be null");
        this.clientBuilder = Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
        this.secretResolver = Objects.requireNonNull(secretResolver, "secretResolver must not be null");
        this.errorSanitizer = Objects.requireNonNull(errorSanitizer, "errorSanitizer must not be null");
    }

    public McpClientHandle create(McpServerProperties server) {
        Objects.requireNonNull(server, "server must not be null");
        McpSafetyValidator.validate(server);
        McpSecretHeaderResolver.ResolvedTransportSecrets secrets = secretResolver.resolve(server);
        McpTransportFactory.TransportHandle transport = transportFactory.create(new McpTransportFactory.TransportRequest(
                server.id(), server.transport(), server, secrets, server.timeout()));
        InitializedClient client = clientBuilder.buildAndInitialize(new ClientBuildRequest(
                server.id(), transport, server.timeout(), true));
        return new McpClientHandle(server.id(), transport.kind(), client);
    }

    @FunctionalInterface
    interface TransportFactory {
        McpTransportFactory.TransportHandle create(McpTransportFactory.TransportRequest request);
    }

    @FunctionalInterface
    interface ClientBuilder {
        InitializedClient buildAndInitialize(ClientBuildRequest request);
    }

    interface InitializedClient extends AutoCloseable {
        @Override
        void close();
    }

    record ClientBuildRequest(String serverId, McpTransportFactory.TransportHandle transport, Duration timeout, boolean minimalToolsOnly) {
        ClientBuildRequest {
            if (serverId == null || serverId.isBlank()) {
                throw new IllegalArgumentException("serverId must not be blank");
            }
            transport = Objects.requireNonNull(transport, "transport must not be null");
            timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        }
    }

    private static final class SdkClientBuilder implements ClientBuilder {
        @Override
        public InitializedClient buildAndInitialize(ClientBuildRequest request) {
            McpSyncClient client = McpClient.sync(request.transport().sdkTransport())
                    .requestTimeout(request.timeout())
                    .capabilities(McpSchema.ClientCapabilities.builder().build())
                    .build();
            client.initialize();
            return client::closeGracefully;
        }
    }
}
