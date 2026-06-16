package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;
import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientFactoryTest {
    private static final String FAKE_SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";

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

    @Test
    void resolvesStaticHeaderAndEnvRefsOnlyAtTransportBoundaryWithoutLeakingSecretMarkers() {
        CapturingTransportFactory transportFactory = new CapturingTransportFactory();
        CapturingClientBuilder clientBuilder = new CapturingClientBuilder();
        McpSecretHeaderResolver resolver = McpSecretHeaderResolver.from(secretRef ->
                Optional.of(ResolvedSecret.sensitive(secretRef, FAKE_SECRET + "-" + secretRef.scheme())));
        McpClientFactory factory = new McpClientFactory(transportFactory, clientBuilder, resolver, McpClientErrorSanitizer.defaults());
        McpAuthProperties auth = new McpAuthProperties(null, "env:MCP_BEARER", "X-Api-Key", "config:pi.mcp.api-key",
                Map.of("X-Custom-Secret", "vault:custom/header"));
        McpServerProperties server = new McpServerProperties(
                "secret-server", true, "Secret Server", McpTransportKind.STREAMABLE_HTTP,
                "https://mcp.example.test", "/mcp", null, List.of(), Map.of("MCP_ENV", "env:MCP_ENV_SECRET"),
                Duration.ofSeconds(10), auth, Map.of());

        McpClientHandle handle = factory.create(server);
        McpSecretHeaderResolver.ResolvedTransportSecrets secrets = transportFactory.requests.getFirst().secrets();

        assertThat(secrets.headers())
                .containsEntry("Authorization", "Bearer " + FAKE_SECRET + "-env")
                .containsEntry("X-Api-Key", FAKE_SECRET + "-config")
                .containsEntry("X-Custom-Secret", FAKE_SECRET + "-vault");
        assertThat(secrets.env()).containsEntry("MCP_ENV", FAKE_SECRET + "-env");
        assertThat(handle.toString()).doesNotContain(FAKE_SECRET);
        assertThat(server.toString()).doesNotContain(FAKE_SECRET);
        assertThat(secrets.toString()).doesNotContain(FAKE_SECRET);
        assertThat(secrets.redactedSummary().toString()).doesNotContain(FAKE_SECRET);
    }

    @Test
    void wrapsInitializationFailuresInSanitizedCategorizedInfrastructureException() {
        CapturingTransportFactory transportFactory = new CapturingTransportFactory();
        McpClientFactory.ClientBuilder failingClientBuilder = request -> {
            throw new RuntimeException("401 Unauthorized body token=" + FAKE_SECRET + " Authorization: Bearer " + FAKE_SECRET);
        };
        McpClientFactory factory = new McpClientFactory(transportFactory, failingClientBuilder, McpSecretHeaderResolver.none(), McpClientErrorSanitizer.defaults());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> factory.create(McpServerProperties.streamableHttp(
                        "auth-server", "Auth Server", "https://mcp.example.test", "/mcp", McpAuthProperties.none(), Map.of())))
                .isInstanceOf(McpClientException.class)
                .hasMessageContaining("auth-server")
                .hasMessageContaining("AUTH_FAILED")
                .hasMessageContaining("Check MCP server credentials")
                .hasMessageNotContaining(FAKE_SECRET)
                .hasMessageNotContaining("Authorization")
                .satisfies(error -> assertThat(((McpClientException) error).category()).isEqualTo(McpClientErrorSanitizer.Category.AUTH_FAILED));
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
        public List<McpSchema.Tool> listTools() {
            return List.of();
        }

        @Override
        public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest request) {
            return new McpSchema.CallToolResult(List.of(), false);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
