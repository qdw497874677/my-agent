package io.github.pi_java.agent.infrastructure.mcp.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void safetyValidatorRejectsUnsafeUrlSchemesBlankHostsAndCredentialUrls() {
        assertThatThrownBy(() -> McpSafetyValidator.validate(McpServerProperties.streamableHttp(
                "file-server",
                "Unsafe File URL",
                "file:///etc/passwd",
                "/mcp",
                McpAuthProperties.none(),
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported URL scheme")
                .hasMessageNotContaining("/etc/passwd");

        assertThatThrownBy(() -> McpSafetyValidator.validate(McpServerProperties.sse(
                "credential-url",
                "Credential URL",
                "https://token:secret@mcp.example.test/sse",
                McpAuthProperties.none(),
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials in URL")
                .hasMessageNotContaining("token:secret");

        assertThatThrownBy(() -> McpSafetyValidator.validate(McpServerProperties.streamableHttp(
                "blank-host",
                "Blank Host",
                "https:///mcp",
                "/mcp",
                McpAuthProperties.none(),
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    @Test
    void safetyValidatorRequiresExplicitStdioCommandAndRejectsRawHeaderSecrets() {
        assertThatThrownBy(() -> McpSafetyValidator.validate(new McpServerProperties(
                "bad-stdio",
                true,
                "Bad stdio",
                McpTransportKind.STDIO,
                null,
                null,
                " ",
                List.of(),
                Map.of(),
                Duration.ofSeconds(10),
                McpAuthProperties.none(),
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stdio command");

        McpAuthProperties auth = McpAuthProperties.none()
                .withCustomHeaderSecretRefs(Map.of("Authorization", "Bearer sk-live-raw-secret-value"));

        assertThatThrownBy(() -> McpSafetyValidator.validate(McpServerProperties.streamableHttp(
                "raw-header",
                "Raw Header",
                "https://mcp.example.test",
                "/mcp",
                auth,
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret reference")
                .hasMessageNotContaining("sk-live-raw-secret-value")
                .hasMessageNotContaining("Bearer");
    }

    @Test
    void safetyValidatorAllowsSafeConfigurations() {
        McpSafetyValidator.ValidationResult result = McpSafetyValidator.validate(McpServerProperties.streamableHttp(
                "safe",
                "Safe MCP",
                "https://mcp.example.test",
                "/mcp",
                McpAuthProperties.bearerTokenRef("env:SAFE_MCP_TOKEN"),
                Map.of("egress", "allowlisted")));

        assertThat(result.valid()).isTrue();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.sanitizedSummary()).containsEntry("transport", "STREAMABLE_HTTP");
        assertThat(result.sanitizedSummary().toString()).doesNotContain("SAFE_MCP_TOKEN");
    }
}
