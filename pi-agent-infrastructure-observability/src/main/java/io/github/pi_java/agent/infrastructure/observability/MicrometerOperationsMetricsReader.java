package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.observability.OperationsMetricsReader;
import io.github.pi_java.agent.client.admin.OperationMetricDto;
import io.github.pi_java.agent.client.admin.OperationalWarningDto;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MicrometerOperationsMetricsReader implements OperationsMetricsReader {

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final PiTelemetryRedactor redactor;

    public MicrometerOperationsMetricsReader(MeterRegistry meterRegistry, Clock clock, PiTelemetryRedactor redactor) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public OperationsSummaryResponse summarize(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<OperationMetricDto> runs = metrics("runs", PiTelemetryNames.RUN_EVENTS_TOTAL);
        List<OperationMetricDto> models = merge(
                metrics("models", PiTelemetryNames.MODEL_CALLS_TOTAL),
                metrics("models", PiTelemetryNames.MODEL_CALL_DURATION));
        List<OperationMetricDto> tools = merge(
                metrics("tools", PiTelemetryNames.TOOL_EXECUTIONS_TOTAL),
                metrics("tools", PiTelemetryNames.TOOL_EXECUTION_DURATION));
        List<OperationMetricDto> policies = metrics("policies", PiTelemetryNames.POLICY_DECISIONS_TOTAL);
        List<OperationMetricDto> mcp = merge(
                metrics("mcp", PiTelemetryNames.MCP_INVOCATIONS_TOTAL),
                metrics("mcp", PiTelemetryNames.MCP_DISCOVERY_DURATION));
        List<OperationMetricDto> plugins = merge(
                metrics("plugins", PiTelemetryNames.PLUGIN_LIFECYCLE_TOTAL),
                metrics("plugins", PiTelemetryNames.PLUGIN_DISCOVERY_DURATION));
        List<OperationMetricDto> errors = collectErrors(runs, models, tools, policies, mcp, plugins);
        return new OperationsSummaryResponse(
                runs,
                models,
                tools,
                policies,
                mcp,
                plugins,
                errors,
                warnings(errors),
                clock.instant());
    }

    private List<OperationMetricDto> metrics(String area, String meterName) {
        return meterRegistry.find(meterName).meters().stream()
                .map(meter -> toMetric(area, meterName, meter))
                .toList();
    }

    private OperationMetricDto toMetric(String area, String meterName, Meter meter) {
        String status = status(meter.getId());
        double value = value(meter);
        String unit = meter instanceof Timer ? "milliseconds" : "count";
        return new OperationMetricDto(area, meterName, status, value, unit, metadata(meter.getId()));
    }

    private double value(Meter meter) {
        if (meter instanceof Counter counter) {
            return counter.count();
        }
        if (meter instanceof Timer timer) {
            return timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        return meter.measure().iterator().hasNext() ? meter.measure().iterator().next().getValue() : 0.0d;
    }

    private String status(Meter.Id id) {
        String status = id.getTag("status");
        if (status == null) {
            status = id.getTag("decision");
        }
        if (status == null) {
            return "HEALTHY";
        }
        String safe = redactor.safeTag(status);
        return "error".equalsIgnoreCase(safe) || "failed".equalsIgnoreCase(safe) || "denied".equalsIgnoreCase(safe)
                ? "DEGRADED"
                : "HEALTHY";
    }

    private Map<String, String> metadata(Meter.Id id) {
        Map<String, String> metadata = new LinkedHashMap<>();
        id.getTags().forEach(tag -> metadata.put(redactor.safeTag(tag.getKey()), redactor.safeTag(tag.getValue())));
        return metadata;
    }

    @SafeVarargs
    private static List<OperationMetricDto> merge(List<OperationMetricDto>... lists) {
        List<OperationMetricDto> merged = new ArrayList<>();
        for (List<OperationMetricDto> list : lists) {
            merged.addAll(list);
        }
        return List.copyOf(merged);
    }

    @SafeVarargs
    private static List<OperationMetricDto> collectErrors(List<OperationMetricDto>... sections) {
        List<OperationMetricDto> errors = new ArrayList<>();
        for (List<OperationMetricDto> section : sections) {
            section.stream()
                    .filter(metric -> "DEGRADED".equals(metric.status()))
                    .forEach(errors::add);
        }
        return List.copyOf(errors);
    }

    private List<OperationalWarningDto> warnings(List<OperationMetricDto> errors) {
        return errors.stream()
                .map(metric -> new OperationalWarningDto(
                        metric.area(),
                        "WARN",
                        warningMessage(metric.area()),
                        Map.of("metric", metric.name(), "status", metric.status())))
                .distinct()
                .toList();
    }

    private static String warningMessage(String area) {
        return switch (area) {
            case "runs" -> "Recent run failures detected";
            case "models" -> "Recent model failures detected";
            case "tools" -> "Recent tool failures detected";
            case "policies" -> "Recent policy blocks detected";
            case "mcp" -> "Recent MCP failures detected";
            case "plugins" -> "Recent plugin lifecycle failures detected";
            default -> "Recent operational failures detected";
        };
    }
}
