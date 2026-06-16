package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
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
}
