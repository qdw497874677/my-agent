package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpToolRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, TestCloudRuntimeConfiguration.class,
        McpToolRegistryWiringTest.FakeMcpDiscoveryConfiguration.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "pi.mcp.discovery.startup=false")
class McpToolRegistryWiringTest {

    @Autowired
    private McpServerRegistry serverRegistry;

    @Autowired
    private McpToolRegistry mcpToolRegistry;

    @Autowired
    private McpGovernanceCatalog mcpGovernanceCatalog;

    @Autowired
    private ToolRegistry toolRegistry;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void mcpGovernanceBeansExistWithSafeEmptyFallback() {
        assertThat(serverRegistry.snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.server().id()).isEqualTo("fake");
            assertThat(snapshot.discoveryStatus()).isIn("NOT_DISCOVERED", "DISCOVERED");
        });
        assertThat(toolRegistry.listTools())
                .extracting("id")
                .contains("builtin.info", "builtin.workspace.write", "builtin.workspace.command");
    }

    @Test
    void mcpToolsResolveThroughCompositeToolRegistryWithMcpProvenanceAfterRefresh() {
        serverRegistry.refresh();

        assertThat(mcpToolRegistry.listTools()).extracting("id").containsExactly("mcp.fake.echo");
        assertThat(toolRegistry.listTools())
                .extracting("id")
                .contains("builtin.info", "mcp.fake.echo");
        assertThat(toolRegistry.resolve("mcp.fake.echo")).isPresent().get().satisfies(resolution -> {
            assertThat(resolution.descriptor().provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.MCP);
            assertThat(resolution.descriptor().provenance().sourceId()).isEqualTo("fake");
        });
        assertThat(mcpGovernanceCatalog.servers()).singleElement().satisfies(server -> {
            assertThat(server.serverId()).isEqualTo("fake");
            assertThat(server.tools()).singleElement().satisfies(tool ->
                    assertThat(tool.serverQualifiedToolId()).isEqualTo("mcp.fake.echo"));
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeMcpDiscoveryConfiguration {
        @Bean
        @Primary
        McpServerRegistry fakeMcpServerRegistry(Clock clock) {
            McpServerProperties server = McpServerProperties.streamableHttp(
                    "fake", "Fake MCP", "https://mcp.fake.test", "/mcp", McpAuthProperties.none(), Map.of());
            return new McpServerRegistry(List.of(server), s -> new McpServerRegistry.DiscoveryClient() {
                @Override
                public List<McpSchema.Tool> listTools() {
                    return List.of(new McpSchema.Tool("echo", null, "Echo input",
                            new McpSchema.JsonSchema("object", Map.of(), List.of(), null, Map.of(), Map.of()),
                            null,
                            new McpSchema.ToolAnnotations("Echo", true, false, true, false, null),
                            Map.of()));
                }

                @Override
                public void close() {
                }
            }, clock);
        }
    }
}
