package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.List;
import java.util.Objects;

public final class TelemetryMcpToolExecutorBinding implements ToolExecutorBinding {
    private final ToolExecutorBinding delegate;
    private final String serverId;
    private final String toolName;
    private final String transportKind;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public TelemetryMcpToolExecutorBinding(ToolExecutorBinding delegate, String serverId, String toolName,
                                           String transportKind, PiTelemetry telemetry) {
        this(delegate, serverId, toolName, transportKind, telemetry, new PiTelemetryRedactor());
    }

    TelemetryMcpToolExecutorBinding(ToolExecutorBinding delegate, String serverId, String toolName, String transportKind,
                                    PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.serverId = serverId;
        this.toolName = toolName;
        this.transportKind = transportKind;
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken cancellationToken) {
        Span span = telemetry.span(PiTelemetryNames.MCP_INVOCATION_SPAN);
        span.setAttribute(PiTelemetryNames.ATTR_MCP_SERVER_ID, redactor.safeTag(serverId));
        span.setAttribute("pi.mcp.tool_name", redactor.safeTag(toolName));
        span.setAttribute("pi.mcp.transport_kind", redactor.safeTag(transportKind));
        String status = "success";
        try (Scope ignored = span.makeCurrent()) {
            ToolExecutionResult result = delegate.execute(request, cancellationToken);
            status = result.status().name();
            return result;
        } catch (RuntimeException ex) {
            status = "error";
            span.recordException(new RuntimeException(ex.getClass().getSimpleName()));
            throw ex;
        } finally {
            List<Tag> tags = List.of(
                    Tag.of("server_id", redactor.safeTag(serverId)),
                    Tag.of("tool_name", redactor.safeTag(toolName)),
                    Tag.of("transport_kind", redactor.safeTag(transportKind)),
                    Tag.of("status", redactor.safeTag(status)));
            telemetry.counter(PiTelemetryNames.MCP_INVOCATIONS_TOTAL, tags).increment();
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, redactor.safeTag(status));
            span.end();
        }
    }
}
