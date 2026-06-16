package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, TestCloudRuntimeConfiguration.class})
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
        assertThat(serverRegistry.snapshots()).isEmpty();
        assertThat(mcpToolRegistry.listTools()).isEmpty();
        assertThat(mcpGovernanceCatalog.servers()).isEmpty();
        assertThat(toolRegistry.listTools())
                .extracting("id")
                .contains("builtin.info", "builtin.workspace.write", "builtin.workspace.command");
    }
}
