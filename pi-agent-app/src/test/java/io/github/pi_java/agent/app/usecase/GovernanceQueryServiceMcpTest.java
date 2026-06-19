package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.github.pi_java.agent.app.port.mcp.McpToolStatus;
import io.github.pi_java.agent.app.port.plugin.EmptyPluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceQueryServiceMcpTest {

    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-1", "admin-1", Set.of("ADMIN")),
            new CorrelationContext("trace-1", "corr-1", null));

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

    @Test
    void mcpQueryMapsHealthyAndFailedServersToPublicDtosWithoutRawSecrets() {
        DefaultGovernanceQueryService service = serviceWith(fakeCatalog());

        McpGovernanceResponse response = service.mcp(CONTEXT);

        assertThat(response.servers()).hasSize(2);
        assertThat(response.servers().getFirst().serverId()).isEqualTo("server-healthy");
        assertThat(response.servers().getFirst().tools()).hasSize(1);
        assertThat(response.servers().getFirst().tools().getFirst().metadata())
                .containsEntry("policy", "read-only");
        assertThat(response.servers().get(1).connectionStatus()).isEqualTo("AUTH_FAILED");
        assertThat(response.servers().get(1).redactedError()).isEqualTo("token:<redacted>");
        assertThat(response.toString()).doesNotContain("raw-secret-token");
    }

    @Test
    void overviewMcpStatusReflectsCatalogCountsAndFailedServers() {
        DefaultGovernanceQueryService service = serviceWith(fakeCatalog());

        GovernanceOverviewResponse overview = service.overview(CONTEXT);

        assertThat(overview.mcp().status()).isEqualTo("AUTH_FAILED");
        assertThat(overview.mcp().count()).isEqualTo(2);
        assertThat(overview.mcp().metadata())
                .containsEntry("surface", "read-only")
                .containsEntry("mutation", "disabled")
                .containsEntry("servers", "2")
                .containsEntry("tools", "1")
                .containsEntry("disabledServers", "1")
                .containsEntry("unhealthyServers", "1");
    }

    @Test
    void refreshMcpMapsRefreshStatusWithoutMutationConfiguration() {
        DefaultGovernanceQueryService service = serviceWith(fakeCatalog());

        McpRefreshResponse response = service.refreshMcp(CONTEXT);

        assertThat(response.refreshed()).isTrue();
        assertThat(response.serverCount()).isEqualTo(2);
        assertThat(response.failedServerCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("AUTH_FAILED");
        assertThat(response.redactedError()).isEqualTo("token:<redacted>");
        assertThat(response.toString()).doesNotContain("raw-secret-token");
    }

    private static DefaultGovernanceQueryService serviceWith(McpGovernanceCatalog mcpCatalog) {
        return new DefaultGovernanceQueryService(
                List::of,
                new ToolRegistry() {
                    @Override
                    public List<io.github.pi_java.agent.domain.tool.ToolDescriptor> listTools() {
                        return List.of();
                    }

                    @Override
                    public Optional<ToolResolution> resolve(String toolId) {
                        return Optional.empty();
                    }
                },
                () -> List.of(),
                mcpCatalog,
                new EmptyPluginGovernanceCatalog(),
                Optional.empty(),
                Optional.empty(),
                Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC));
    }

    private static McpGovernanceCatalog fakeCatalog() {
        McpToolStatus tool = new McpToolStatus(
                "server-healthy:files.read",
                "files.read",
                "AVAILABLE",
                true,
                false,
                false,
                "object{path:string}",
                "",
                Map.of("policy", "read-only", "secret", "<redacted>"));
        McpServerStatus healthy = new McpServerStatus(
                "server-healthy",
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
                Map.of("auth", "token:<redacted>"));
        McpServerStatus failed = new McpServerStatus(
                "server-failed",
                "Broken Remote MCP",
                false,
                "sse",
                "token:<redacted>",
                "AUTH_FAILED",
                "DISCOVERY_FAILED",
                0,
                Instant.parse("2026-06-15T12:00:00Z"),
                "token:<redacted>",
                List.of(),
                Map.of("failure", "token:<redacted>"));
        return new McpGovernanceCatalog() {
            @Override
            public List<McpServerStatus> servers() {
                return List.of(healthy, failed);
            }

            @Override
            public McpRefreshStatus refresh() {
                return new McpRefreshStatus(
                        true,
                        2,
                        1,
                        1,
                        "AUTH_FAILED",
                        "token:<redacted>",
                        Map.of("startedBy", "admin", "secret", "<redacted>"));
            }
        };
    }
}
