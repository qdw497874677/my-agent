package io.github.pi_java.agent.infrastructure.mcp.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerPropertiesTest {

    @Test
    void defaultsToStreamableHttpAndExposesRedactedGovernanceSummary() {
        McpServerProperties server = McpServerProperties.streamableHttp(
                "github",
                "GitHub MCP",
                "https://mcp.example.test",
                "/mcp",
                McpAuthProperties.bearerTokenRef("env:GITHUB_MCP_TOKEN"),
                Map.of("tier", "trusted"));

        assertThat(server.id()).isEqualTo("github");
        assertThat(server.enabled()).isTrue();
        assertThat(server.transport()).isEqualTo(McpTransportKind.STREAMABLE_HTTP);
        assertThat(server.endpoint()).isEqualTo("/mcp");
        assertThat(server.auth().publicSummary()).containsEntry("auth", "bearer-token-ref");
        assertThat(server.auth().publicSummary().toString()).doesNotContain("GITHUB_MCP_TOKEN");

        Map<String, String> summary = server.publicSummary();
        assertThat(summary)
                .containsEntry("id", "github")
                .containsEntry("transport", "STREAMABLE_HTTP")
                .containsEntry("auth", "bearer-token-ref")
                .containsEntry("headers", "0")
                .containsEntry("enabled", "true");
        assertThat(summary.toString()).doesNotContain("GITHUB_MCP_TOKEN");
    }

    @Test
    void representsExplicitStdioAndLegacySseTrustedServers() {
        McpServerProperties stdio = McpServerProperties.stdio(
                "filesystem",
                "Filesystem MCP",
                "node",
                List.of("/opt/mcp/filesystem.js"),
                Map.of("API_TOKEN", "env:FILESYSTEM_MCP_TOKEN"),
                Duration.ofSeconds(15),
                Map.of("network", "local-process"));
        McpServerProperties sse = McpServerProperties.sse(
                "legacy",
                "Legacy MCP",
                "https://legacy.example.test/sse",
                McpAuthProperties.apiKeyHeaderRef("X-Api-Key", "config:pi.mcp.legacy.api-key"),
                Map.of());

        assertThat(stdio.transport()).isEqualTo(McpTransportKind.STDIO);
        assertThat(stdio.command()).isEqualTo("node");
        assertThat(stdio.envSecretRefs()).containsEntry("API_TOKEN", "env:FILESYSTEM_MCP_TOKEN");
        assertThat(stdio.publicSummary())
                .containsEntry("transport", "STDIO")
                .containsEntry("envRefs", "1");
        assertThat(stdio.publicSummary().toString()).doesNotContain("FILESYSTEM_MCP_TOKEN");

        assertThat(sse.transport()).isEqualTo(McpTransportKind.SSE);
        assertThat(sse.auth().publicSummary())
                .containsEntry("auth", "api-key-ref")
                .containsEntry("headers", "1");
        assertThat(sse.publicSummary().toString()).doesNotContain("pi.mcp.legacy.api-key");
    }
}
