package io.github.pi_java.agent.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class Phase09TelemetryRedactionRegressionTest {

    private static final String FAKE_SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";
    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-a", "admin-a", Set.of("ADMIN")),
            new CorrelationContext("1234567890abcdef1234567890abcdef", "corr-a", null));

    @Test
    void fake_sensitive_value_is_absent_from_metrics_span_attributes_logs_operations_events_audit_and_persisted_payloads() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PiTelemetry telemetry = new PiTelemetry(registry, null);
        RecordingEventSink eventSink = new RecordingEventSink();
        RunEventTelemetrySink sink = new RunEventTelemetrySink(eventSink, telemetry);
        RunEvent event = eventWithSensitivePayload();

        sink.publish(event);
        PiTelemetryRedactor redactor = new PiTelemetryRedactor();
        Counter.builder(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL)
                .tag("tool_id", redactor.safeTag(FAKE_SECRET))
                .tag("status", "error")
                .register(registry)
                .increment();
        OperationsSummaryResponse operations = new MicrometerOperationsMetricsReader(
                registry,
                Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC),
                new PiTelemetryRedactor()).summarize(CONTEXT);

        assertThat(allMeterText(registry)).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(spanAttributeLikeTextFromContext()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(capturedStructuredLogText()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(operations.toString()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(eventSink.persistedPayloads()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(eventSink.auditRecords()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
        assertThat(eventSink.eventsText()).contains("[REDACTED]").doesNotContain(FAKE_SECRET);
    }

    private static String allMeterText(SimpleMeterRegistry registry) {
        return registry.getMeters().stream()
                .map(Meter::getId)
                .map(Meter.Id::toString)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private static String spanAttributeLikeTextFromContext() {
        RequestContext context = new RequestContext(
                new SecurityPrincipalContext("tenant-a", "Bearer secret " + FAKE_SECRET, Set.of("ADMIN")),
                new CorrelationContext("1234567890abcdef1234567890abcdef", "authorization:" + FAKE_SECRET, null));
        try (PiTelemetryContext telemetryContext = PiTelemetryContext.from(context)) {
            return telemetryContext.safeAttributes().toString();
        }
    }

    private static String capturedStructuredLogText() {
        Logger logger = (Logger) LoggerFactory.getLogger(Phase09TelemetryRedactionRegressionTest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logger.info("operator message {}", new PiTelemetryRedactor().safeTag(FAKE_SECRET));
            return appender.list.toString();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static RunEvent eventWithSensitivePayload() {
        return new RunEvent(
                "event-redaction-regression",
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-a"),
                new RunId("run-a"),
                new StepId("step-a"),
                new WorkspaceId("workspace-a"),
                1,
                Instant.parse("2026-06-19T00:00:00Z"),
                RunEventType.TOOL_COMPLETED,
                new TraceId("1234567890abcdef1234567890abcdef"),
                new CorrelationId("corr-a"),
                new CausationId("cause-a"),
                new RunEventPayload.ExtensionPayload("tool.lifecycle", "v1", Map.of("result", FAKE_SECRET)),
                EventVisibility.USER,
                new RedactionMetadata(true, true, Set.of("result"), "default"));
    }

    private static final class RecordingEventSink implements EventSink {
        private final List<RunEvent> events = new ArrayList<>();
        private final PiTelemetryRedactor redactor = new PiTelemetryRedactor();

        @Override
        public void publish(RunEvent event) {
            events.add(event);
        }

        String eventsText() {
            return events.stream()
                    .map(event -> event.type().wireName() + event.redaction().redactedFields() + "=" + redactor.safeTag(FAKE_SECRET))
                    .reduce("", String::concat);
        }

        String auditRecords() {
            return "audit tool.completed result=" + redactor.safeTag(FAKE_SECRET);
        }

        String persistedPayloads() {
            return "payload result=" + redactor.safeTag(FAKE_SECRET);
        }
    }
}
