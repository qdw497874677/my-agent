package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.event.RunEvent;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PiTelemetryContext implements AutoCloseable {

    private static final PiTelemetryRedactor REDACTOR = new PiTelemetryRedactor();

    private final Map<String, String> previousValues;
    private final Map<String, String> safeAttributes;
    private boolean closed;

    private PiTelemetryContext(Map<String, String> values) {
        this.previousValues = new LinkedHashMap<>();
        this.safeAttributes = sanitize(values);
        values.forEach(this::putMdcValue);
    }

    public static PiTelemetryContext from(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("traceId", context.traceId());
        values.put("correlationId", context.correlationId());
        values.put("tenantId", context.tenantId());
        values.put("userId", context.userId());
        return new PiTelemetryContext(values);
    }

    public static PiTelemetryContext from(RunEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sessionId", event.sessionId().value());
        values.put("runId", event.runId().value());
        values.put("traceId", event.traceId().value());
        values.put("correlationId", event.correlationId().value());
        values.put("tenantId", event.tenantId().value());
        values.put("userId", event.userId().value());
        return new PiTelemetryContext(values);
    }

    public Map<String, String> safeAttributes() {
        return safeAttributes;
    }

    public Map<String, String> safeSpanAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, PiTelemetryNames.ATTR_TRACE_ID, safeAttributes.get("traceId"));
        putIfPresent(attributes, PiTelemetryNames.ATTR_CORRELATION_ID, safeAttributes.get("correlationId"));
        putIfPresent(attributes, PiTelemetryNames.ATTR_TENANT_ID, safeAttributes.get("tenantId"));
        putIfPresent(attributes, PiTelemetryNames.ATTR_USER_ID, safeAttributes.get("userId"));
        putIfPresent(attributes, PiTelemetryNames.ATTR_SESSION_ID, safeAttributes.get("sessionId"));
        putIfPresent(attributes, PiTelemetryNames.ATTR_RUN_ID, safeAttributes.get("runId"));
        return Map.copyOf(attributes);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        for (Map.Entry<String, String> entry : previousValues.entrySet()) {
            if (entry.getValue() == null) {
                MDC.remove(entry.getKey());
            } else {
                MDC.put(entry.getKey(), entry.getValue());
            }
        }
        closed = true;
    }

    private void putMdcValue(String key, String value) {
        previousValues.put(key, MDC.get(key));
        String safeValue = REDACTOR.safeTag(value);
        if (PiTelemetryRedactor.UNKNOWN.equals(safeValue)) {
            MDC.remove(key);
        } else {
            MDC.put(key, safeValue);
        }
    }

    private static Map<String, String> sanitize(Map<String, String> values) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> sanitized.put(key, REDACTOR.safeTag(value)));
        return Map.copyOf(sanitized);
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (value != null && !PiTelemetryRedactor.UNKNOWN.equals(value)) {
            attributes.put(key, value);
        }
    }
}
