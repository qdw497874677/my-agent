package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PiTelemetryContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void request_context_writes_and_restores_explicit_mdc_values() {
        MDC.put("traceId", "previous-trace");
        RequestContext context = new RequestContext(
                new SecurityPrincipalContext("tenant-a", "user-a", Set.of("USER")),
                new CorrelationContext("4bf92f3577b34da6a3ce929d0e0e4736", "corr-a", "cause-a"));

        try (PiTelemetryContext ignored = PiTelemetryContext.from(context)) {
            assertThat(MDC.get("traceId")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(MDC.get("correlationId")).isEqualTo("corr-a");
            assertThat(MDC.get("tenantId")).isEqualTo("tenant-a");
            assertThat(MDC.get("userId")).isEqualTo("user-a");
        }

        assertThat(MDC.get("traceId")).isEqualTo("previous-trace");
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void run_event_context_writes_and_cleans_up_mdc_after_close() {
        RunEvent event = sampleEvent();

        PiTelemetryContext context = PiTelemetryContext.from(event);
        assertThat(MDC.get("sessionId")).isEqualTo("session-a");
        assertThat(MDC.get("runId")).isEqualTo("run-a");
        assertThat(MDC.get("traceId")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(MDC.get("correlationId")).isEqualTo("corr-a");
        assertThat(MDC.get("tenantId")).isEqualTo("tenant-a");
        assertThat(MDC.get("userId")).isEqualTo("user-a");

        context.close();

        assertThat(MDC.get("sessionId")).isNull();
        assertThat(MDC.get("runId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void sensitive_values_do_not_survive_as_safe_tags_or_mdc_values() {
        RequestContext context = new RequestContext(
                new SecurityPrincipalContext("tenant-a", "Bearer secret-token", Set.of()),
                new CorrelationContext("4bf92f3577b34da6a3ce929d0e0e4736", "authorization: api_key=abc", null));

        try (PiTelemetryContext telemetryContext = PiTelemetryContext.from(context)) {
            assertThat(MDC.get("userId")).isEqualTo(PiTelemetryRedactor.REDACTED);
            assertThat(MDC.get("correlationId")).isEqualTo(PiTelemetryRedactor.REDACTED);
            assertThat(telemetryContext.safeAttributes())
                    .containsEntry("userId", PiTelemetryRedactor.REDACTED)
                    .containsEntry("correlationId", PiTelemetryRedactor.REDACTED);
        }
    }

    @Test
    void telemetry_facade_uses_simple_meter_registry_and_noop_span_safely() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PiTelemetry telemetry = new PiTelemetry(registry, null);

        telemetry.counter(PiTelemetryNames.RUN_EVENTS_TOTAL, Tags.of("event_type", "run.created")).increment();

        assertThat(registry.get(PiTelemetryNames.RUN_EVENTS_TOTAL).counter().count()).isEqualTo(1.0d);
        assertThat(telemetry.span(PiTelemetryNames.RUN_SPAN).getSpanContext().isValid()).isFalse();
    }

    private static RunEvent sampleEvent() {
        return new RunEvent(
                "event-a",
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-a"),
                new RunId("run-a"),
                new StepId("step-a"),
                new WorkspaceId("workspace-a"),
                1,
                Instant.parse("2026-06-19T00:00:00Z"),
                RunEventType.RUN_CREATED,
                new TraceId("4bf92f3577b34da6a3ce929d0e0e4736"),
                new CorrelationId("corr-a"),
                new CausationId("cause-a"),
                new RunEventPayload.RunLifecyclePayload(RunStatus.QUEUED, null),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }
}
