package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TelemetryRunDispatcher implements RunDispatcher {

    private final RunDispatcher delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public TelemetryRunDispatcher(RunDispatcher delegate, PiTelemetry telemetry) {
        this(delegate, telemetry, new PiTelemetryRedactor());
    }

    TelemetryRunDispatcher(RunDispatcher delegate, PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    public RunDispatcher delegate() {
        return delegate;
    }

    @Override
    public void dispatch(String workerId) {
        record("dispatch", workerId, null, () -> delegate.dispatch(workerId));
    }

    @Override
    public void dispatchRun(String workerId, String runId) {
        record("dispatchRun", workerId, runId, () -> delegate.dispatchRun(workerId, runId));
    }

    private void record(String operation, String workerId, String runId, DispatchCall call) {
        Timer.Sample sample = telemetry.startTimer();
        Span span = telemetry.span(PiTelemetryNames.RUN_DISPATCH_SPAN);
        Map<String, String> previousMdc = putMdc(workerId, runId);
        String status = "success";
        try {
            span.setAttribute("operation", operation);
            span.setAttribute("worker_id", safe(workerId));
            if (runId != null) {
                span.setAttribute(PiTelemetryNames.ATTR_RUN_ID, safe(runId));
            }
            call.run();
        } catch (RuntimeException | Error failure) {
            status = "error";
            span.recordException(failure);
            throw failure;
        } finally {
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
            telemetry.stopTimer(sample, PiTelemetryNames.RUN_DISPATCH_DURATION, tags(operation, status, workerId, runId));
            span.end();
            restoreMdc(previousMdc);
        }
    }

    private Iterable<Tag> tags(String operation, String status, String workerId, String runId) {
        java.util.ArrayList<Tag> tags = new java.util.ArrayList<>(List.of(
                Tag.of("operation", operation),
                Tag.of("status", status),
                Tag.of("worker_id", safe(workerId))
        ));
        if (runId != null) {
            tags.add(Tag.of("run_id", safe(runId)));
        }
        return tags;
    }

    private String safe(String value) {
        return redactor.safeTag(value);
    }

    private Map<String, String> putMdc(String workerId, String runId) {
        Map<String, String> previous = new LinkedHashMap<>();
        putMdc(previous, "workerId", workerId);
        if (runId != null) {
            putMdc(previous, "runId", runId);
        }
        return previous;
    }

    private void putMdc(Map<String, String> previous, String key, String value) {
        previous.put(key, MDC.get(key));
        String safeValue = safe(value);
        if (PiTelemetryRedactor.UNKNOWN.equals(safeValue)) {
            MDC.remove(key);
        } else {
            MDC.put(key, safeValue);
        }
    }

    private static void restoreMdc(Map<String, String> previous) {
        previous.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }

    @FunctionalInterface
    private interface DispatchCall {
        void run();
    }
}
