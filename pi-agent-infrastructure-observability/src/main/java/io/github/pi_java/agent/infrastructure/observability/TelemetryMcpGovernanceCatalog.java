package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.mcp.McpRefreshStatus;
import io.github.pi_java.agent.app.port.mcp.McpServerStatus;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.List;
import java.util.Objects;

public final class TelemetryMcpGovernanceCatalog implements McpGovernanceCatalog {
    private final McpGovernanceCatalog delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public TelemetryMcpGovernanceCatalog(McpGovernanceCatalog delegate, PiTelemetry telemetry) {
        this(delegate, telemetry, new PiTelemetryRedactor());
    }

    TelemetryMcpGovernanceCatalog(McpGovernanceCatalog delegate, PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public List<McpServerStatus> servers() {
        return timedDiscovery("servers", delegate::servers);
    }

    @Override
    public McpRefreshStatus refresh() {
        return timedDiscovery("refresh", delegate::refresh);
    }

    private <T> T timedDiscovery(String action, java.util.function.Supplier<T> supplier) {
        Timer.Sample sample = telemetry.startTimer();
        Span span = telemetry.span("pi.mcp.discovery");
        span.setAttribute("pi.mcp.action", redactor.safeTag(action));
        String status = "success";
        try (Scope ignored = span.makeCurrent()) {
            T result = supplier.get();
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
            if (result instanceof List<?> list) {
                span.setAttribute("pi.mcp.server_count", list.size());
            } else if (result instanceof McpRefreshStatus refreshStatus) {
                span.setAttribute("pi.mcp.server_count", refreshStatus.serverCount());
            }
            return result;
        } catch (RuntimeException ex) {
            status = "error";
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, status);
            span.recordException(new RuntimeException(ex.getClass().getSimpleName()));
            throw ex;
        } finally {
            sample.stop(Timer.builder(PiTelemetryNames.MCP_DISCOVERY_DURATION)
                    .tags(List.of(Tag.of("action", redactor.safeTag(action)), Tag.of("status", redactor.safeTag(status))))
                    .register(telemetry.meterRegistry()));
            span.end();
        }
    }
}
