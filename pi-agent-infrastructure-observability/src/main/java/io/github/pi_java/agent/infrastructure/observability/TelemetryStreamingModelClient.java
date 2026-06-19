package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.List;
import java.util.Objects;

public final class TelemetryStreamingModelClient implements StreamingModelClient {
    private final StreamingModelClient delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public TelemetryStreamingModelClient(StreamingModelClient delegate, PiTelemetry telemetry) {
        this(delegate, telemetry, new PiTelemetryRedactor());
    }

    TelemetryStreamingModelClient(StreamingModelClient delegate, PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public void stream(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink) {
        Objects.requireNonNull(request, "request must not be null");
        ProviderModelRef ref = safeModelRef(request);
        Timer.Sample sample = telemetry.startTimer();
        Span span = telemetry.span(PiTelemetryNames.MODEL_CALL_SPAN);
        span.setAttribute(PiTelemetryNames.ATTR_PROVIDER_ID, redactor.safeTag(ref.providerId()));
        span.setAttribute("pi.model_id", redactor.safeTag(ref.modelId()));
        String status = "success";
        try (Scope ignored = span.makeCurrent()) {
            delegate.stream(request, cancellationToken, sink);
        } catch (RuntimeException ex) {
            status = "error";
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
            span.recordException(new RuntimeException(ex.getClass().getSimpleName()));
            throw ex;
        } finally {
            List<Tag> tags = tags(ref, status);
            telemetry.counter(PiTelemetryNames.MODEL_CALLS_TOTAL, tags).increment();
            sample.stop(Timer.builder(PiTelemetryNames.MODEL_CALL_DURATION).tags(tags).register(telemetry.meterRegistry()));
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
            span.end();
        }
    }

    private List<Tag> tags(ProviderModelRef ref, String status) {
        return List.of(
                Tag.of("provider", redactor.safeTag(ref.providerId())),
                Tag.of("model", redactor.safeTag(ref.modelId())),
                Tag.of("status", redactor.safeTag(status)));
    }

    private ProviderModelRef safeModelRef(ModelRequest request) {
        try {
            return ProviderModelRef.parse(request.context().agentDefinition().modelRef());
        } catch (RuntimeException ex) {
            return new ProviderModelRef(PiTelemetryRedactor.UNKNOWN, PiTelemetryRedactor.UNKNOWN);
        }
    }
}
