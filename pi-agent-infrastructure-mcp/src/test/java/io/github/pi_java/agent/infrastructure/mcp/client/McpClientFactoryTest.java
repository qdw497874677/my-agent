package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientFactoryTest {

    @Test
    void createsInitializedHandlesForConfiguredTransportKindsWithMinimalCapabilities() {
        CapturingTransportFactory transportFactory = new CapturingTransportFactory();
        CapturingClientBuilder clientBuilder = new CapturingClientBuilder();
        McpClientFactory factory = new McpClientFactory(transportFactory, clientBuilder, McpSecretHeaderResolver.none(), McpClientErrorSanitizer.defaults());

        McpClientHandle http = factory.create(McpServerProperties.streamableHttp(
                "http-server", "HTTP Server", "https://mcp.example.test", "/mcp", McpAuthProperties.none(), Map.of()));
        McpClientHandle sse = factory.create(McpServerProperties.sse(
                "sse-server", "SSE Server", "https://legacy.example.test/sse", McpAuthProperties.none(), Map.of()));
        McpClientHandle stdio = factory.create(McpServerProperties.stdio(
                "stdio-server", "Stdio Server", "node", List.of("server.js"), Map.of(), Duration.ofSeconds(12), Map.of()));

        assertThat(http.serverId()).isEqualTo("http-server");
        assertThat(http.transportKind()).isEqualTo(McpTransportKind.STREAMABLE_HTTP);
        assertThat(sse.transportKind()).isEqualTo(McpTransportKind.SSE);
        assertThat(stdio.transportKind()).isEqualTo(McpTransportKind.STDIO);
        assertThat(clientBuilder.requests).extracting(McpClientFactory.ClientBuildRequest::minimalToolsOnly).containsOnly(true);
        assertThat(clientBuilder.requests).extracting(McpClientFactory.ClientBuildRequest::timeout).contains(Duration.ofSeconds(30), Duration.ofSeconds(12));
        assertThat(transportFactory.requests).extracting(McpTransportFactory.TransportRequest::kind)
                .containsExactly(McpTransportKind.STREAMABLE_HTTP, McpTransportKind.SSE, McpTransportKind.STDIO);

        http.close();
        assertThat(clientBuilder.clients.getFirst().closed).isTrue();
    }

    static final class CapturingTransportFactory implements McpClientFactory.TransportFactory {
        final List<McpTransportFactory.TransportRequest> requests = new ArrayList<>();

        @Override
        public McpTransportFactory.TransportHandle create(McpTransportFactory.TransportRequest request) {
            requests.add(request);
            return new McpTransportFactory.TransportHandle(request.kind(), new Object());
        }
    }

    static final class CapturingClientBuilder implements McpClientFactory.ClientBuilder {
        final List<McpClientFactory.ClientBuildRequest> requests = new ArrayList<>();
        final List<CapturingInitializedClient> clients = new ArrayList<>();

        @Override
        public McpClientFactory.InitializedClient buildAndInitialize(McpClientFactory.ClientBuildRequest request) {
            requests.add(request);
            CapturingInitializedClient client = new CapturingInitializedClient();
            clients.add(client);
            return client;
        }
    }

    static final class CapturingInitializedClient implements McpClientFactory.InitializedClient {
        boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
