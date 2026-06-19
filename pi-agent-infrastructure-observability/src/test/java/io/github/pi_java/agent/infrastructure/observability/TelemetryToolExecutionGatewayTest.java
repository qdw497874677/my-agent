package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway.ToolExecutionCommand;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryToolExecutionGatewayTest {

    private static final String SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void records_success_metrics_without_raw_arguments_or_output_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PiTelemetry telemetry = new PiTelemetry(registry, null);
        TelemetryToolExecutionGateway gateway = new TelemetryToolExecutionGateway(command ->
                new ToolExecutionResult(command.request().toolCallId(), command.request().toolId(), ToolExecutionStatus.SUCCESS,
                        "completed", Optional.empty(), Map.of("visible", "safe"), Map.of("redacted", "safe"), Set.of("apiKey"),
                        Optional.empty(), Duration.ofMillis(7), Optional.of(Map.of("raw", SECRET))), telemetry);

        ToolExecutionResult result = gateway.execute(command(Map.of("apiKey", SECRET)));

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL).counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTION_DURATION).timer().count()).isEqualTo(1L);
        assertMeterTagsDoNotLeakSecret(registry);
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL).meter().getId().getTag("tool_id")).isEqualTo("demo.tool");
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL).meter().getId().getTag("status")).isEqualTo("SUCCESS");
    }

    @Test
    void records_error_metrics_and_rethrows_without_raw_arguments_as_metric_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PiTelemetry telemetry = new PiTelemetry(registry, null);
        TelemetryToolExecutionGateway gateway = new TelemetryToolExecutionGateway(command -> {
            throw new IllegalStateException("boom " + SECRET);
        }, telemetry);

        assertThatThrownBy(() -> gateway.execute(command(Map.of("password", SECRET))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL).counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTION_DURATION).timer().count()).isEqualTo(1L);
        assertThat(registry.get(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL).meter().getId().getTag("status")).isEqualTo("error");
        assertMeterTagsDoNotLeakSecret(registry);
    }

    private static ToolExecutionCommand command(Map<String, Object> arguments) {
        return new ToolExecutionCommand(
                new RequestContext(new SecurityPrincipalContext("tenant-a", "user-a", Set.of("USER")),
                        new CorrelationContext("4bf92f3577b34da6a3ce929d0e0e4736", "corr-a", "cause-a")),
                new SessionId("session-a"),
                new WorkspaceId("workspace-a"),
                new ToolExecutionRequest("call-a", new RunId("run-a"), new StepId("step-a"), "demo.tool", "v1",
                        arguments, Instant.parse("2026-06-19T00:00:00Z")),
                new CancellationToken());
    }

    private static void assertMeterTagsDoNotLeakSecret(SimpleMeterRegistry registry) {
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags().stream().map(tag -> tag.getKey() + "=" + tag.getValue()).toList())
                .noneMatch(value -> value.contains(SECRET))
                .noneMatch(value -> value.contains("apiKey"))
                .noneMatch(value -> value.contains("password"));
        assertThat(registry.getMeters().stream().map(Meter::getId).map(Meter.Id::getName).toList())
                .contains(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL, PiTelemetryNames.TOOL_EXECUTION_DURATION);
    }
}
