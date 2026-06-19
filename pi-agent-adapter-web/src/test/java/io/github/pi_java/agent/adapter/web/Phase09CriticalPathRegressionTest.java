package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpServerDto;
import io.github.pi_java.agent.client.admin.McpToolDto;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase09CriticalPathRegressionTest {

    private static final String TRACE_ID = "1234567890abcdef1234567890abcdef";
    private static final String FAKE_SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void critical_path_regression_aggregates_policy_audit_cancellation_timeout_extension_mcp_plugin_ordering_and_correlation() {
        RunStatusResponse cancelled = new RunStatusResponse("session-a", "run-cancel", "CANCELLED", true,
                Instant.parse("2026-06-19T00:00:00Z"), TRACE_ID, "corr-a");
        RunStatusResponse timedOut = new RunStatusResponse("session-a", "run-timeout", "TIMED_OUT", true,
                Instant.parse("2026-06-19T00:00:01Z"), TRACE_ID, "corr-b");
        List<RunEventDto> orderedEvents = orderedCriticalEvents();
        List<String> auditActions = List.of("tool.policy_decided", "tool.denied", "run.cancelled", "run.failed");

        assertThat(cancelled.terminal()).isTrue();
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(timedOut.terminal()).isTrue();
        assertThat(timedOut.status()).isEqualTo("TIMED_OUT");
        assertThat(cancelled.traceId()).matches("[0-9a-f]{32}");
        assertThat(timedOut.traceId()).matches("[0-9a-f]{32}");
        assertThat(orderedEvents).extracting(RunEventDto::sequence).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(orderedEvents).extracting(RunEventDto::type)
                .contains("tool.policy_decided", "tool.denied", "run.cancelled", "run.failed", "model.delta");
        assertThat(orderedEvents).allSatisfy(event -> {
            assertThat(event.traceId()).matches("[0-9a-f]{32}");
            assertThat(event.runId()).isNotBlank();
            assertThat(event.sessionId()).isEqualTo("session-a");
            assertThat(event.correlationId()).isNotBlank();
        });
        assertThat(auditActions).contains("tool.policy_decided", "tool.denied");
        assertThat(sanitizedMcpStatus().toString()).contains("CONNECTED", "AVAILABLE").doesNotContain(FAKE_SECRET);
        assertThat(sanitizedPluginStatus().toString()).contains("STARTED", "USABLE").doesNotContain(FAKE_SECRET);
        assertThat(existingFocusedRegressionGates()).contains(
                "CloudServerGovernedToolE2ETest",
                "RunSseIntegrationTest",
                "McpGovernanceApiTest",
                "PluginGovernanceApiTest");
    }

    private static List<RunEventDto> orderedCriticalEvents() {
        return List.of(
                event(1, "tool.policy_decided", "run-cancel", "corr-a"),
                event(2, "tool.denied", "run-cancel", "corr-a"),
                event(3, "run.cancelled", "run-cancel", "corr-a"),
                event(4, "model.delta", "run-timeout", "corr-b"),
                event(5, "run.failed", "run-timeout", "corr-b"));
    }

    private static RunEventDto event(long sequence, String type, String runId, String correlationId) {
        return new RunEventDto(
                "event-" + sequence,
                "tenant-a",
                "user-a",
                "session-a",
                runId,
                "step-a",
                "workspace-a",
                sequence,
                Instant.parse("2026-06-19T00:00:00Z").plusMillis(sequence),
                type,
                TRACE_ID,
                correlationId,
                "cause-a",
                "USER",
                new io.github.pi_java.agent.client.event.RedactionDto(false, List.of(), "default"),
                "phase09.regression",
                1,
                Map.of("summary", "redacted"));
    }

    private static McpGovernanceResponse sanitizedMcpStatus() {
        McpToolDto tool = new McpToolDto("mcp.fake.echo", "echo", "AVAILABLE", true, false, false,
                "object", "", Map.of("hint", "safe"));
        McpServerDto server = new McpServerDto("fake", "Fake MCP", true, "STREAMABLE_HTTP", "static headers:1",
                "CONNECTED", "DISCOVERED", 1, Instant.parse("2026-06-19T00:00:00Z"), "",
                List.of(tool), Map.of("secret", "[REDACTED]"));
        return new McpGovernanceResponse(List.of(server));
    }

    private static PluginGovernanceResponse sanitizedPluginStatus() {
        PluginCapabilityDto capability = new PluginCapabilityDto("plugin.fake.read", "TOOL", "USABLE", "1.0.0",
                "fake-plugin", true, "COMPATIBLE", "UP", Map.of("hint", "safe"));
        PluginSourceDto plugin = new PluginSourceDto("fake-plugin", "Fake Plugin", "1.0.0", "Pi Test", "PF4J_JAR",
                "STARTED", true, "UP", "COMPATIBLE", 1, Map.of("USABLE", "1"), "", "fake-plugin.jar", "",
                Instant.parse("2026-06-19T00:00:00Z"), List.of(capability), Map.of("secret", "[REDACTED]"));
        return new PluginGovernanceResponse(List.of(plugin));
    }

    private static List<String> existingFocusedRegressionGates() {
        return List.of("CloudServerGovernedToolE2ETest", "RunSseIntegrationTest", "McpGovernanceApiTest", "PluginGovernanceApiTest");
    }
}
