package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
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

    private static McpSchema.Tool tool(String name, String description, Map<String, Object> inputSchema,
                                      McpSchema.ToolAnnotations annotations) {
        return new McpSchema.Tool(name, null, description,
                new McpSchema.JsonSchema((String) inputSchema.getOrDefault("type", "object"), Map.of(), List.of(), null, Map.of(), Map.of()),
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
