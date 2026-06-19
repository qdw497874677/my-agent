package io.github.pi_java.agent.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.util.Objects;

public final class PiTelemetry {

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public PiTelemetry(MeterRegistry meterRegistry, Tracer tracer) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.tracer = tracer;
    }

    public Counter counter(String name, Iterable<Tag> tags) {
        return Counter.builder(requireName(name)).tags(tags == null ? java.util.List.of() : tags).register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String name, Iterable<Tag> tags) {
        Objects.requireNonNull(sample, "sample must not be null");
        sample.stop(Timer.builder(requireName(name)).tags(tags == null ? java.util.List.of() : tags).register(meterRegistry));
    }

    public MeterRegistry meterRegistry() {
        return meterRegistry;
    }

    public Span span(String spanName) {
        if (tracer == null) {
            return Span.getInvalid();
        }
        return tracer.spanBuilder(requireName(spanName)).startSpan();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("telemetry name must not be blank");
        }
        return name;
    }
}
