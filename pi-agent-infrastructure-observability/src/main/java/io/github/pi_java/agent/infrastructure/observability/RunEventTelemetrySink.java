package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.trace.Span;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RunEventTelemetrySink implements EventSink {

    public static final String RUN_EVENT_SPAN = "pi.run.event";

    private final EventSink delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public RunEventTelemetrySink(EventSink delegate, PiTelemetry telemetry) {
        this(delegate, telemetry, new PiTelemetryRedactor());
    }

    RunEventTelemetrySink(EventSink delegate, PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public void publish(RunEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        try (PiTelemetryContext context = PiTelemetryContext.from(event)) {
            recordEventMetric(event);
            Span span = telemetry.span(RUN_EVENT_SPAN);
            try {
                applySafeSpanAttributes(span, event, context.safeSpanAttributes());
                delegate.publish(event);
            } catch (RuntimeException | Error failure) {
                span.recordException(failure);
                throw failure;
            } finally {
                span.end();
            }
        }
    }

    private void recordEventMetric(RunEvent event) {
        telemetry.counter(PiTelemetryNames.RUN_EVENTS_TOTAL, List.of(
                Tag.of("event_type", safeTag(event.type().wireName())),
                Tag.of("terminal", Boolean.toString(isTerminal(event.type()))),
                Tag.of("source", "run_event"),
                Tag.of("status", status(event.type()))
        )).increment();
    }

    private void applySafeSpanAttributes(Span span, RunEvent event, Map<String, String> contextAttributes) {
        span.setAttribute(PiTelemetryNames.ATTR_EVENT_TYPE, safeTag(event.type().wireName()));
        contextAttributes.forEach(span::setAttribute);
    }

    private String safeTag(String value) {
        return redactor.safeTag(value);
    }

    private static boolean isTerminal(RunEventType type) {
        return type == RunEventType.RUN_COMPLETED
                || type == RunEventType.RUN_FAILED
                || type == RunEventType.RUN_CANCELLED
                || type == RunEventType.RUN_POLICY_BLOCKED;
    }

    private static String status(RunEventType type) {
        return switch (type) {
            case RUN_COMPLETED -> "completed";
            case RUN_FAILED, TOOL_FAILED -> "failed";
            case RUN_CANCELLED, TOOL_CANCELLED -> "cancelled";
            case RUN_POLICY_BLOCKED, POLICY_DECIDED, TOOL_POLICY_DECIDED, TOOL_DENIED, TOOL_APPROVAL_REQUIRED -> "policy";
            default -> "unknown";
        };
    }
}
