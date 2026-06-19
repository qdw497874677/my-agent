package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.Map;
import java.util.Objects;

public final class TelemetryToolExecutionGateway implements ToolExecutionGateway {

    private static final String ATTR_POLICY_DECISION = "pi.policy_decision";

    private final ToolExecutionGateway delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor = new PiTelemetryRedactor();

    public TelemetryToolExecutionGateway(ToolExecutionGateway delegate, PiTelemetry telemetry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Timer.Sample sample = telemetry.startTimer();
        try (PiTelemetryContext telemetryContext = PiTelemetryContext.from(command.context())) {
            Span span = telemetry.span(PiTelemetryNames.TOOL_EXECUTION_SPAN);
            try (Scope ignored = span.makeCurrent()) {
                addContextAttributes(span, telemetryContext.safeSpanAttributes());
                span.setAttribute(PiTelemetryNames.ATTR_TOOL_ID, safe(command.request().toolId()));
                ToolExecutionResult result = delegate.execute(command);
                String status = safe(result.status().name());
                String policyDecision = policyDecision(result);
                Tags tags = tags(command.request().toolId(), status, policyDecision);
                telemetry.counter(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL, tags).increment();
                telemetry.stopTimer(sample, PiTelemetryNames.TOOL_EXECUTION_DURATION, tags);
                span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
                span.setAttribute(ATTR_POLICY_DECISION, policyDecision);
                return result;
            } catch (RuntimeException ex) {
                Tags tags = tags(command.request().toolId(), "error", "unknown");
                telemetry.counter(PiTelemetryNames.TOOL_EXECUTIONS_TOTAL, tags).increment();
                telemetry.stopTimer(sample, PiTelemetryNames.TOOL_EXECUTION_DURATION, tags);
                span.setAttribute(PiTelemetryNames.ATTR_STATUS, "error");
                span.setAttribute(ATTR_POLICY_DECISION, "unknown");
                span.recordException(ex);
                throw ex;
            } finally {
                span.end();
            }
        }
    }

    public ToolExecutionGateway delegate() {
        return delegate;
    }

    private Tags tags(String toolId, String status, String policyDecision) {
        return Tags.of(
                Tag.of("tool_id", safe(toolId)),
                Tag.of("status", safe(status)),
                Tag.of("policy_decision", safe(policyDecision))
        );
    }

    private String policyDecision(ToolExecutionResult result) {
        return result.errorCategory()
                .filter(value -> value.equalsIgnoreCase("DENY") || value.equalsIgnoreCase("BLOCK")
                        || value.equalsIgnoreCase("REQUIRE_APPROVAL") || value.equalsIgnoreCase("REQUIRE_SANDBOX"))
                .map(String::toLowerCase)
                .map(this::safe)
                .orElse(PiTelemetryRedactor.UNKNOWN);
    }

    private void addContextAttributes(Span span, Map<String, String> attributes) {
        attributes.forEach(span::setAttribute);
    }

    private String safe(String value) {
        return redactor.safeTag(value);
    }
}
