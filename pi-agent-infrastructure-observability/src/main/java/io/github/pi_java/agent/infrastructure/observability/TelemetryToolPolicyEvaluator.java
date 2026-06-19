package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.Map;
import java.util.Objects;

public final class TelemetryToolPolicyEvaluator implements ToolPolicyEvaluator {

    private static final String ATTR_POLICY_REF = "pi.policy_ref";
    private static final String ATTR_DECISION = "pi.policy_decision";

    private final ToolPolicyEvaluator delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor = new PiTelemetryRedactor();

    public TelemetryToolPolicyEvaluator(ToolPolicyEvaluator delegate, PiTelemetry telemetry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    @Override
    public PolicyEvaluation evaluate(PolicyEvaluationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try (PiTelemetryContext telemetryContext = PiTelemetryContext.from(request.context())) {
            Span span = telemetry.span(PiTelemetryNames.POLICY_DECISION_SPAN);
            try (Scope ignored = span.makeCurrent()) {
                addContextAttributes(span, telemetryContext.safeSpanAttributes());
                span.setAttribute(PiTelemetryNames.ATTR_TOOL_ID, safe(request.descriptor().id()));
                PolicyEvaluation evaluation = delegate.evaluate(request);
                String decision = safe(evaluation.decision().name());
                String policyRef = safe(evaluation.policyRef());
                Tags tags = tags(request.descriptor().id(), decision, policyRef, "success");
                telemetry.counter(PiTelemetryNames.POLICY_DECISIONS_TOTAL, tags).increment();
                span.setAttribute(PiTelemetryNames.ATTR_STATUS, "success");
                span.setAttribute(ATTR_DECISION, decision);
                span.setAttribute(ATTR_POLICY_REF, policyRef);
                return evaluation;
            } catch (RuntimeException ex) {
                Tags tags = tags(request.descriptor().id(), "error", "unknown", "error");
                telemetry.counter(PiTelemetryNames.POLICY_DECISIONS_TOTAL, tags).increment();
                span.setAttribute(PiTelemetryNames.ATTR_STATUS, "error");
                span.setAttribute(ATTR_DECISION, "error");
                span.setAttribute(ATTR_POLICY_REF, "unknown");
                span.recordException(ex);
                throw ex;
            } finally {
                span.end();
            }
        }
    }

    public ToolPolicyEvaluator delegate() {
        return delegate;
    }

    private Tags tags(String toolId, String decision, String policyRef, String status) {
        return Tags.of(
                Tag.of("tool_id", safe(toolId)),
                Tag.of("decision", safe(decision)),
                Tag.of("policy_ref", safe(policyRef)),
                Tag.of("status", safe(status))
        );
    }

    private void addContextAttributes(Span span, Map<String, String> attributes) {
        attributes.forEach(span::setAttribute);
    }

    private String safe(String value) {
        return redactor.safeTag(value);
    }
}
