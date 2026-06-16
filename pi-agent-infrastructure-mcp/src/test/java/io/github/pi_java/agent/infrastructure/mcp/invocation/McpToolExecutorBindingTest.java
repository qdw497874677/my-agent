package io.github.pi_java.agent.infrastructure.mcp.invocation;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolExecutorBindingTest {
    private static final String FAKE_SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void invokesConfiguredMcpToolNameWithRequestArgumentsOnly() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        clients.nextResult(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("done")), false));
        McpToolExecutorBinding binding = new McpToolExecutorBinding(server("github"), "search", clients);

        ToolExecutionResult result = binding.execute(request("mcp.github.search",
                Map.of("query", "pi", "tool", "malicious-overwrite")), new CancellationToken());

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(clients.requests()).singleElement().satisfies(call -> {
            assertThat(call.serverId()).isEqualTo("github");
            assertThat(call.request().name()).isEqualTo("search");
            assertThat(call.request().arguments()).containsEntry("query", "pi");
            assertThat(call.request().arguments()).containsEntry("tool", "malicious-overwrite");
        });
    }

    @Test
    void returnsCancelledResultWithoutRemoteInvocationWhenTokenAlreadyCancelled() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        CancellationToken token = new CancellationToken();
        token.cancel("user cancelled");

        ToolExecutionResult result = new McpToolExecutorBinding(server("github"), "search", clients)
                .execute(request("mcp.github.search", Map.of("query", "pi")), token);

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.CANCELLED);
        assertThat(result.errorCategory()).contains("MCP_INVOCATION_CANCELLED");
        assertThat(clients.requests()).isEmpty();
    }

    @Test
    void mapsTextAndStructuredContentToSafeSuccessSummary() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        clients.nextResult(new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("hello")), false,
                Map.of("answer", 42, "items", List.of("a", "b")), Map.of("remoteMeta", "ignored")));

        ToolExecutionResult result = new McpToolExecutorBinding(server("github"), "search", clients)
                .execute(request("mcp.github.search", Map.of("query", "pi")), new CancellationToken());

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.summary()).contains("MCP tool github/search completed");
        assertThat(result.redactedOutputSummary())
                .containsEntry("mcp.serverId", "github")
                .containsEntry("mcp.toolName", "search")
                .containsEntry("mcp.contentCount", 1)
                .containsEntry("mcp.redactionHint", "summary-only");
        assertThat(result.rawOutput()).isPresent().get().satisfies(raw -> {
            assertThat(raw).containsKey("mcp.content");
            assertThat(raw).containsEntry("mcp.structuredContent", Map.of("answer", 42, "items", List.of("a", "b")));
        });
    }

    @Test
    void mapsMixedMcpContentToSourceNeutralSummariesWithoutBinaryPayloads() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        clients.nextResult(new McpSchema.CallToolResult(List.of(
                new McpSchema.TextContent("plain text"),
                new McpSchema.ImageContent(null, "BASE64_IMAGE_DATA_SHOULD_NOT_SURFACE", "image/png"),
                McpSchema.ResourceLink.builder()
                        .name("report")
                        .uri("mcp://github/report/1")
                        .mimeType("application/json")
                        .build()), false));

        ToolExecutionResult result = new McpToolExecutorBinding(server("github"), "search", clients)
                .execute(request("mcp.github.search", Map.of("query", "pi")), new CancellationToken());

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.redactedOutputSummary())
                .containsEntry("mcp.contentCount", 3)
                .containsEntry("mcp.contentTypes", List.of("text", "image", "resource_link"));
        assertThat(result.toString()).doesNotContain("BASE64_IMAGE_DATA_SHOULD_NOT_SURFACE");
        assertThat(result.rawOutput()).isPresent().get().satisfies(raw -> assertThat(raw.toString())
                .contains("dataRedacted=true")
                .contains("mcp://github/report/1")
                .doesNotContain("BASE64_IMAGE_DATA_SHOULD_NOT_SURFACE"));
    }

    @Test
    void mapsMcpErrorContentToStableFailureWithoutRawRemoteBody() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        clients.nextResult(new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("remote exploded token=" + FAKE_SECRET)), true));

        ToolExecutionResult result = new McpToolExecutorBinding(server("github"), "search", clients)
                .execute(request("mcp.github.search", Map.of("query", "pi")), new CancellationToken());

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(result.errorCategory()).contains("MCP_TOOL_ERROR");
        assertThat(result.toString()).doesNotContain(FAKE_SECRET).doesNotContain("remote exploded");
        assertThat(result.redactedOutputSummary())
                .containsEntry("mcp.serverId", "github")
                .containsEntry("mcp.toolName", "search")
                .containsEntry("mcp.redactionHint", "remote-error-redacted");
    }

    @Test
    void normalizesInvocationFailuresAndRedactsSecretMarkers() {
        FakeInvocationClientFactory clients = new FakeInvocationClientFactory();
        clients.nextFailure(new RuntimeException("401 Authorization failed bearer=" + FAKE_SECRET));

        ToolExecutionResult result = new McpToolExecutorBinding(server("github"), "search", clients)
                .execute(request("mcp.github.search", Map.of("query", "pi")), new CancellationToken());

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(result.errorCategory()).contains("MCP_AUTH_FAILED");
        assertThat(result.toString()).doesNotContain(FAKE_SECRET).doesNotContain("bearer=");
        assertThat(result.redactedOutputSummary())
                .containsEntry("mcp.serverId", "github")
                .containsEntry("mcp.toolName", "search")
                .containsEntry("mcp.actionHint", "Check MCP server credentials and authorization scope.");
    }

    private static McpServerProperties server(String id) {
        return McpServerProperties.streamableHttp(id, id, "https://mcp.example.test", "/mcp",
                McpAuthProperties.none(), Map.of());
    }

    private static ToolExecutionRequest request(String toolId, Map<String, Object> arguments) {
        return new ToolExecutionRequest("call-1", new RunId("run-1"), new StepId("step-1"), toolId,
                "mcp", arguments, Instant.parse("2026-06-16T09:00:00Z"));
    }

    private static final class FakeInvocationClientFactory implements McpToolExecutorBinding.InvocationClientFactory {
        private final List<ObservedCall> requests = new ArrayList<>();
        private McpSchema.CallToolResult nextResult;
        private RuntimeException nextFailure;

        void nextResult(McpSchema.CallToolResult result) {
            this.nextResult = result;
        }

        void nextFailure(RuntimeException failure) {
            this.nextFailure = failure;
        }

        List<ObservedCall> requests() {
            return List.copyOf(requests);
        }

        @Override
        public McpToolExecutorBinding.InvocationClient open(McpServerProperties server) {
            return new McpToolExecutorBinding.InvocationClient() {
                @Override
                public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest request) {
                    requests.add(new ObservedCall(server.id(), request));
                    if (nextFailure != null) {
                        throw nextFailure;
                    }
                    return nextResult;
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private record ObservedCall(String serverId, McpSchema.CallToolRequest request) {
    }
}
