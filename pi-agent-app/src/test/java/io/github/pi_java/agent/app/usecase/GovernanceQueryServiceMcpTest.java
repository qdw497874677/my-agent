package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.github.pi_java.agent.app.port.mcp.McpToolStatus;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceQueryServiceMcpTest {

    @Test
    void mcpGovernanceCatalogContractsAreFrameworkFreeAndExposeRefresh() throws Exception {
        Method servers = McpGovernanceCatalog.class.getMethod("servers");
        Method refresh = McpGovernanceCatalog.class.getMethod("refresh");

        assertThat(servers.getReturnType()).isEqualTo(List.class);
        assertThat(refresh.getReturnType()).isEqualTo(McpRefreshStatus.class);
        assertThat(McpGovernanceCatalog.class.getMethods())
                .extracting(Method::getName)
                .contains("overallStatus");

        McpToolStatus tool = new McpToolStatus(
                "server-1:files.read",
                "files.read",
                "AVAILABLE",
                true,
                false,
                false,
                "object{path:string}",
                "",
                Map.of("policy", "read-only"));
        McpServerStatus server = new McpServerStatus(
                "server-1",
                "Filesystem MCP",
                true,
                "stdio",
                "token:<redacted>",
                "HEALTHY",
                "HEALTHY",
                1,
                Instant.parse("2026-06-15T12:00:00Z"),
                "",
                List.of(tool),
                Map.of("tenant", "tenant-1"));
        McpGovernanceCatalog catalog = new McpGovernanceCatalog() {
            @Override
            public List<McpServerStatus> servers() {
                return List.of(server);
            }

            @Override
            public McpRefreshStatus refresh() {
                return new McpRefreshStatus(true, 1, 1, 0, "HEALTHY", "", Map.of("trigger", "admin"));
            }
        };

        assertThat(catalog.servers().getFirst().tools().getFirst().serverQualifiedToolId())
                .isEqualTo("server-1:files.read");
        assertThat(catalog.overallStatus()).isEqualTo("HEALTHY");
        assertThat(catalog.refresh().metadata()).containsEntry("trigger", "admin");
    }

    @Test
    void emptyMcpGovernanceCatalogReportsUnconfiguredReadOnlyState() {
        McpGovernanceCatalog catalog = new McpGovernanceCatalog.EmptyMcpGovernanceCatalog();

        assertThat(catalog.servers()).isEmpty();
        assertThat(catalog.overallStatus()).isEqualTo("UNCONFIGURED");
        assertThat(catalog.refresh().status()).isEqualTo("UNCONFIGURED");
        assertThat(catalog.refresh().refreshed()).isFalse();
    }
}
