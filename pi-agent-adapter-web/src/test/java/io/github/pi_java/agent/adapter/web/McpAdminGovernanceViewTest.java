package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminRegistryStatusView;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import io.github.pi_java.agent.client.admin.McpServerDto;
import io.github.pi_java.agent.client.admin.McpToolDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpAdminGovernanceViewTest {

    @Test
    void consoleHttpClientExposesReadOnlyMcpGovernanceStatusAndRefreshHelpers() {
        ConsoleHttpClient client = new ConsoleHttpClient();

        assertThat(client.adminMcpGovernancePath()).isEqualTo("/api/admin/governance/mcp");
        assertThat(client.adminMcpGovernanceResponseType()).isEqualTo(McpGovernanceResponse.class);
        assertThat(client.adminMcpRefreshPath()).isEqualTo("/api/admin/governance/mcp/refresh");
        assertThat(client.adminMcpRefreshResponseType()).isEqualTo(McpRefreshResponse.class);

        assertThat(ConsoleHttpClient.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("adminMcpCreatePath", "adminMcpEditPath", "adminMcpDeletePath", "adminMcpDisablePath");
    }

    @Test
    void registryStatusViewRendersMcpServerToolsRefreshAndNoConfigCrud() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showMcpGovernance(sampleMcpGovernance());

        assertThat(view.mcpGovernancePath()).isEqualTo("/api/admin/governance/mcp");
        assertThat(view.mcpRefreshPath()).isEqualTo("/api/admin/governance/mcp/refresh");
        assertThat(view.mcpRefreshActionText()).contains("Refresh MCP discovery");
        assertThat(view.renderedText())
                .contains("MCP Server: fake")
                .contains("name=Fake MCP")
                .contains("transport=streamable-http")
                .contains("connection=CONNECTED")
                .contains("discovery=DISCOVERED")
                .contains("auth=bearerTokenRef:1")
                .contains("tools=1")
                .contains("lastRefresh=2026-06-16T09:00:00Z")
                .contains("MCP Tool: mcp.fake.echo")
                .contains("availability=AVAILABLE")
                .contains("readOnly=true")
                .contains("destructive=false")
                .contains("openWorld=true")
                .contains("schema=JSON Schema draft 2020-12")
                .contains("MCP Server: failed")
                .contains("connection=UNHEALTHY")
                .contains("error=[REDACTED] connection refused")
                .doesNotContain("PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK")
                .doesNotContain("Add MCP")
                .doesNotContain("Edit MCP")
                .doesNotContain("Delete MCP")
                .doesNotContain("Disable MCP");
        assertThat(view.mutationControlsPresent()).isFalse();
    }

    private static McpGovernanceResponse sampleMcpGovernance() {
        McpToolDto echo = new McpToolDto(
                "mcp.fake.echo",
                "echo",
                "AVAILABLE",
                true,
                false,
                true,
                "JSON Schema draft 2020-12",
                "",
                Map.of("risk", "low"));
        McpServerDto healthy = new McpServerDto(
                "fake",
                "Fake MCP",
                true,
                "streamable-http",
                "bearerTokenRef:1",
                "CONNECTED",
                "DISCOVERED",
                1,
                Instant.parse("2026-06-16T09:00:00Z"),
                "",
                List.of(echo),
                Map.of("surface", "read-only"));
        McpServerDto failed = new McpServerDto(
                "failed",
                "Failed MCP",
                true,
                "sse",
                "none",
                "UNHEALTHY",
                "FAILED",
                0,
                null,
                "[REDACTED] connection refused",
                List.of(),
                Map.of("operatorHint", "check endpoint and credentials"));
        return new McpGovernanceResponse(List.of(healthy, failed));
    }
}
