package io.github.pi_java.agent.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MicrometerOperationsMetricsReaderTest {

    private static final String PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";
    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-a", "admin-a", Set.of("ADMIN")),
            new CorrelationContext("1234567890abcdef1234567890abcdef", "corr-a", null));

    @Test
    void summarizesKnownPiMetersIntoOperationsSections() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter.builder(PiTelemetryNames.RUN_EVENTS_TOTAL)
                .tag("event_type", "run.completed")
                .tag("status", "success")
                .register(registry)
                .increment(2.0d);
        Timer.builder(PiTelemetryNames.MODEL_CALL_DURATION)
                .tag("provider", "openai-compatible")
                .tag("model", "general")
                .tag("status", "success")
                .register(registry)
                .record(Duration.ofMillis(250));
        Counter.builder(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL)
                .tag("tool_id", "builtin.read_info")
                .tag("status", "success")
                .register(registry)
                .increment();
        Counter.builder(PiTelemetryNames.POLICY_DECISIONS_TOTAL)
                .tag("decision", "allow")
                .register(registry)
                .increment();
        Counter.builder(PiTelemetryNames.MCP_INVOCATIONS_TOTAL)
                .tag("server_id", "filesystem")
                .tag("status", "success")
                .register(registry)
                .increment();
        Counter.builder(PiTelemetryNames.PLUGIN_LIFECYCLE_TOTAL)
                .tag("plugin_id", "sample-plugin")
                .tag("status", "success")
                .register(registry)
                .increment();

        OperationsSummaryResponse response = reader(registry).summarize(CONTEXT);

        assertThat(response.runs()).extracting("area").containsOnly("runs");
        assertThat(response.models()).extracting("area").containsOnly("models");
        assertThat(response.tools()).extracting("area").containsOnly("tools");
        assertThat(response.policies()).extracting("area").containsOnly("policies");
        assertThat(response.mcp()).extracting("area").containsOnly("mcp");
        assertThat(response.plugins()).extracting("area").containsOnly("plugins");
        assertThat(response.generatedAt()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
    }

    @Test
    void redactsSensitiveMeterTagsAndCreatesGenericWarningsForErrors() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter.builder(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL)
                .tag("tool_id", PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK)
                .tag("status", "error")
                .register(registry)
                .increment();

        OperationsSummaryResponse response = reader(registry).summarize(CONTEXT);

        assertThat(response.toString())
                .contains("[REDACTED]")
                .doesNotContain(PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK);
        assertThat(response.errors()).isNotEmpty();
        assertThat(response.warnings()).anySatisfy(warning -> {
            assertThat(warning.area()).isEqualTo("tools");
            assertThat(warning.message()).isEqualTo("Recent tool failures detected");
        });
    }

    @Test
    void absentMetersProduceEmptySafeSummary() {
        OperationsSummaryResponse response = reader(new SimpleMeterRegistry()).summarize(CONTEXT);

        assertThat(response.runs()).isEmpty();
        assertThat(response.models()).isEmpty();
        assertThat(response.tools()).isEmpty();
        assertThat(response.policies()).isEmpty();
        assertThat(response.mcp()).isEmpty();
        assertThat(response.plugins()).isEmpty();
        assertThat(response.errors()).isEmpty();
        assertThat(response.warnings()).isEmpty();
    }

    private static MicrometerOperationsMetricsReader reader(SimpleMeterRegistry registry) {
        return new MicrometerOperationsMetricsReader(
                registry,
                Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC),
                new PiTelemetryRedactor());
    }
}
