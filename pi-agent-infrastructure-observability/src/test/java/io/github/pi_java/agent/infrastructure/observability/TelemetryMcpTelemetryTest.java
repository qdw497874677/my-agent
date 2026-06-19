package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryMcpTelemetryTest {
    private static final String SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void mcp_refresh_and_servers_record_discovery_duration_without_remote_secret_leakage() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        McpGovernanceCatalog delegate = new McpGovernanceCatalog() {
            @Override
            public List<McpServerStatus> servers() {
                return List.of(new McpServerStatus("github", "GitHub", true, "STREAMABLE_HTTP", "credentialRef:config",
                        "UP", "DISCOVERED", 1, Instant.EPOCH, "remote body " + SECRET, List.of(),
                        Map.of("url", "https://example.invalid/" + SECRET)));
            }

            @Override
            public McpRefreshStatus refresh() {
                return new McpRefreshStatus(true, 1, 1, 0, "REFRESHED", "remote body " + SECRET,
                        Map.of("authorization", "Bearer " + SECRET));
            }
        };
        TelemetryMcpGovernanceCatalog catalog = new TelemetryMcpGovernanceCatalog(delegate, new PiTelemetry(registry, null));

        catalog.servers();
        catalog.refresh();

        assertThat(registry.get(PiTelemetryNames.MCP_DISCOVERY_DURATION).tags("action", "servers", "status", "success")
                .timer().count()).isEqualTo(1L);
        assertThat(registry.get(PiTelemetryNames.MCP_DISCOVERY_DURATION).tags("action", "refresh", "status", "success")
                .timer().count()).isEqualTo(1L);
        assertThat(allMeterTagValues(registry)).doesNotContain(SECRET, "authorization", "Bearer");
    }

    @Test
    void mcp_invocation_counter_uses_only_server_tool_transport_and_status_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryMcpToolExecutorBinding binding = new TelemetryMcpToolExecutorBinding((request, token) ->
                new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                        "remote body " + SECRET, Optional.empty(), Map.of("input", SECRET), Map.of("output", SECRET),
                        Set.of("authorization"), Optional.empty(), Duration.ofMillis(2), Optional.empty()),
                "github", "search", "STREAMABLE_HTTP", new PiTelemetry(registry, null));

        binding.execute(new ToolExecutionRequest("call-a", new RunId("run-a"), new StepId("step-a"),
                "mcp.github.search", "v1", Map.of("query", SECRET), Instant.now()), new CancellationToken());

        assertThat(registry.get(PiTelemetryNames.MCP_INVOCATIONS_TOTAL)
                .tags("server_id", "github", "tool_name", "search", "transport_kind", "STREAMABLE_HTTP", "status", "SUCCESS")
                .counter().count()).isEqualTo(1.0d);
        assertThat(allMeterTagValues(registry)).doesNotContain(SECRET, "authorization", "Bearer");
    }

    private static String allMeterTagValues(SimpleMeterRegistry registry) {
        StringBuilder values = new StringBuilder();
        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> values.append(tag.getValue()).append('\n'));
        }
        return values.toString();
    }
}
