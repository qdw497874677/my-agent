package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolRegistryTest {
    private static final String FAKE_SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-16T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void refreshKeepsConfiguredServerVisibleWhenDiscoveryFailsAndMarksStaleToolsUnavailable() {
        FakeDiscoveryClientFactory clients = new FakeDiscoveryClientFactory();
        McpServerProperties healthy = McpServerProperties.streamableHttp(
                "github", "GitHub MCP", "https://mcp.example.test", "/mcp", McpAuthProperties.none(), Map.of("team", "platform"));
        McpServerRegistry registry = new McpServerRegistry(List.of(healthy), clients, FIXED_CLOCK);
        clients.respond("github", List.of(tool("search", "Search repos", Map.of("type", "object"), null)));

        McpDiscoveryResult firstRefresh = registry.refresh();

        assertThat(firstRefresh.success()).isTrue();
        assertThat(registry.snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.server().id()).isEqualTo("github");
            assertThat(snapshot.connectionStatus()).isEqualTo("CONNECTED");
            assertThat(snapshot.discoveryStatus()).isEqualTo("DISCOVERED");
            assertThat(snapshot.tools()).extracting(McpServerRegistry.DiscoveredTool::name).containsExactly("search");
        });

        clients.fail("github", new RuntimeException("401 Unauthorized token=" + FAKE_SECRET));

        McpDiscoveryResult failedRefresh = registry.refresh();

        assertThat(failedRefresh.success()).isFalse();
        assertThat(failedRefresh.failedServers()).isEqualTo(1);
        assertThat(failedRefresh.redactedError()).doesNotContain(FAKE_SECRET).contains("AUTH_FAILED");
        assertThat(registry.snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.server().id()).isEqualTo("github");
            assertThat(snapshot.connectionStatus()).isEqualTo("AUTH_FAILED");
            assertThat(snapshot.discoveryStatus()).isEqualTo("DISCOVERY_FAILED");
            assertThat(snapshot.redactedError()).doesNotContain(FAKE_SECRET);
            assertThat(snapshot.tools()).singleElement().satisfies(tool -> {
                assertThat(tool.name()).isEqualTo("search");
                assertThat(tool.available()).isFalse();
                assertThat(tool.redactedError()).doesNotContain(FAKE_SECRET).contains("AUTH_FAILED");
            });
        });
    }

    @Test
    void toolRegistryAndGovernanceAdapterExposeOnlyAvailableToolsAndRedactedServerStatus() {
        FakeDiscoveryClientFactory clients = new FakeDiscoveryClientFactory();
        McpServerProperties healthy = McpServerProperties.streamableHttp(
                "github", "GitHub MCP", "https://mcp.example.test", "/mcp", McpAuthProperties.none(), Map.of("team", "platform"));
        McpServerProperties failing = McpServerProperties.streamableHttp(
                "jira", "Jira MCP", "https://jira.example.test", "/mcp", McpAuthProperties.none(), Map.of());
        McpServerRegistry serverRegistry = new McpServerRegistry(List.of(healthy, failing), clients, FIXED_CLOCK);
        clients.respond("github", List.of(tool("search", "Search repos", Map.of("type", "object"),
                new McpSchema.ToolAnnotations("Search", true, false, true, false, null))));
        clients.fail("jira", new RuntimeException("403 Forbidden apiKey=" + FAKE_SECRET));
        serverRegistry.refresh();
        McpToolRegistry toolRegistry = new McpToolRegistry(serverRegistry, new McpToolDescriptorMapper());
        McpGovernanceCatalogAdapter governance = new McpGovernanceCatalogAdapter(serverRegistry, new McpToolDescriptorMapper());

        assertThat(toolRegistry.listTools()).extracting("id").containsExactly("mcp.github.search");
        assertThat(toolRegistry.resolve("mcp.github.search")).isPresent().get().satisfies(resolution -> {
            assertThat(resolution.descriptor().id()).isEqualTo("mcp.github.search");
            ToolExecutionResult result = resolution.executor().execute(null, new CancellationToken());
            assertThat(result.status()).isEqualTo(ToolExecutionStatus.FAILED);
            assertThat(result.errorCategory()).contains("MCP_EXECUTION_DEFERRED");
        });
        assertThat(toolRegistry.resolve("mcp.jira.anything")).isEmpty();

        assertThat(governance.servers()).hasSize(2);
        assertThat(governance.servers()).filteredOn(server -> server.serverId().equals("jira")).singleElement().satisfies(server -> {
            assertThat(server.connectionStatus()).isEqualTo("AUTH_FAILED");
            assertThat(server.discoveryStatus()).isEqualTo("DISCOVERY_FAILED");
            assertThat(server.redactedError()).doesNotContain(FAKE_SECRET).contains("AUTH_FAILED");
            assertThat(server.tools()).isEmpty();
        });
        assertThat(governance.servers()).filteredOn(server -> server.serverId().equals("github")).singleElement().satisfies(server -> {
            assertThat(server.toolCount()).isEqualTo(1);
            assertThat(server.tools()).singleElement().satisfies(tool -> {
                assertThat(tool.serverQualifiedToolId()).isEqualTo("mcp.github.search");
                assertThat(tool.availabilityStatus()).isEqualTo("AVAILABLE");
                assertThat(tool.readOnly()).isTrue();
                assertThat(tool.destructive()).isFalse();
                assertThat(tool.openWorld()).isFalse();
            });
        });
        assertThat(governance.refresh().refreshed()).isTrue();
    }

    @Test
    void mapsMcpToolsToServerQualifiedDescriptorsWithSchemaPassthroughAndPolicyHints() {
        McpServerProperties server = new McpServerProperties(
                "github", true, "GitHub MCP", io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind.STREAMABLE_HTTP,
                "https://mcp.example.test", "/mcp", null, List.of(), Map.of(), Duration.ofSeconds(12),
                McpAuthProperties.none(), Map.of("owner", "platform"));
        McpToolDescriptorMapper mapper = new McpToolDescriptorMapper();
        McpSchema.Tool readOnlyTool = tool("search", "Search repositories",
                Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")), "required", List.of("query")),
                new McpSchema.ToolAnnotations("Search", true, false, true, false, null));

        var descriptor = mapper.toDescriptor(server, new McpServerRegistry.DiscoveredTool(readOnlyTool, true, ""));

        assertThat(descriptor.id()).isEqualTo("mcp.github.search");
        assertThat(descriptor.name()).isEqualTo("search");
        assertThat(descriptor.inputSchema().dialect()).isEqualTo("json-schema");
        assertThat(descriptor.inputSchema().document())
                .containsEntry("type", "object")
                .containsKey("properties")
                .containsEntry("required", List.of("query"));
        assertThat(descriptor.outputSchema()).isEmpty();
        assertThat(descriptor.provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.MCP);
        assertThat(descriptor.provenance().sourceId()).isEqualTo("github");
        assertThat(descriptor.provenance().bindingRef()).isEqualTo("mcp:github:search");
        assertThat(descriptor.version()).isEqualTo("mcp");
        assertThat(descriptor.scopes()).containsExactlyInAnyOrder("tool:mcp", "mcp:server:github", "mcp:tool:github:search");
        assertThat(descriptor.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
        assertThat(descriptor.sideEffect()).isEqualTo(ToolSideEffect.READ_ONLY);
        assertThat(descriptor.defaultTimeout()).isEqualTo(Duration.ofSeconds(12));
        assertThat(descriptor.metadata())
                .containsEntry("mcp.serverId", "github")
                .containsEntry("mcp.toolName", "search")
                .containsEntry("mcp.readOnlyHint", true)
                .containsEntry("mcp.destructiveHint", false)
                .containsEntry("mcp.idempotentHint", true)
                .containsEntry("mcp.openWorldHint", false)
                .containsEntry("mcp.remote", true)
                .containsEntry("mcp.external", true);

        var destructive = mapper.toDescriptor(server, new McpServerRegistry.DiscoveredTool(
                tool("delete_repo", "Delete repository", Map.of("type", "object"),
                        new McpSchema.ToolAnnotations("Delete", false, true, false, true, null)), true, ""));
        assertThat(destructive.riskLevel()).isEqualTo(ToolRiskLevel.HIGH);
        assertThat(destructive.sideEffect()).isEqualTo(ToolSideEffect.DESTRUCTIVE);
        assertThat(destructive.metadata()).containsEntry("mcp.openWorldHint", true);
    }

    private static McpSchema.Tool tool(String name, String description, Map<String, Object> inputSchema,
                                      McpSchema.ToolAnnotations annotations) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.getOrDefault("properties", Map.of());
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.getOrDefault("required", List.of());
        return new McpSchema.Tool(name, null, description,
                new McpSchema.JsonSchema((String) inputSchema.getOrDefault("type", "object"), properties, required, null, Map.of(), Map.of()),
                null, annotations, Map.of());
    }

    static final class FakeDiscoveryClientFactory implements McpServerRegistry.DiscoveryClientFactory {
        private final java.util.Map<String, Object> outcomes = new java.util.LinkedHashMap<>();

        void respond(String serverId, List<McpSchema.Tool> tools) {
            outcomes.put(serverId, tools);
        }

        void fail(String serverId, RuntimeException failure) {
            outcomes.put(serverId, failure);
        }

        @Override
        public McpServerRegistry.DiscoveryClient create(McpServerProperties server) {
            Object outcome = outcomes.get(server.id());
            if (outcome instanceof RuntimeException failure) {
                throw failure;
            }
            @SuppressWarnings("unchecked")
            List<McpSchema.Tool> tools = outcome instanceof List<?> ? (List<McpSchema.Tool>) outcome : List.of();
            return new McpServerRegistry.DiscoveryClient() {
                @Override
                public List<McpSchema.Tool> listTools() {
                    return tools;
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
