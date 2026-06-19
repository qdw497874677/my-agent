package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TelemetryPluginGovernanceCatalog implements PluginGovernanceCatalog {
    private final PluginGovernanceCatalog delegate;
    private final PiTelemetry telemetry;
    private final PiTelemetryRedactor redactor;

    public TelemetryPluginGovernanceCatalog(PluginGovernanceCatalog delegate, PiTelemetry telemetry) {
        this(delegate, telemetry, new PiTelemetryRedactor());
    }

    TelemetryPluginGovernanceCatalog(PluginGovernanceCatalog delegate, PiTelemetry telemetry, PiTelemetryRedactor redactor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
    }

    @Override
    public List<PluginSourceStatus> plugins() {
        return record("plugins", "all", () -> delegate.plugins(), true);
    }

    @Override
    public PluginMutationStatus refresh() {
        PluginMutationStatus status = record("refresh", "all", () -> delegate.refresh(), true);
        return status;
    }

    @Override
    public PluginMutationStatus disable(String pluginId, String actor, String reason) {
        return record("disable", pluginId, () -> delegate.disable(pluginId, actor, reason), false);
    }

    @Override
    public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
        return record("quarantine", pluginId, () -> delegate.quarantine(pluginId, actor, reason), false);
    }

    private <T> T record(String action, String pluginId, Supplier<T> supplier, boolean discoveryTimer) {
        Timer.Sample sample = telemetry.startTimer();
        Span span = telemetry.span(PiTelemetryNames.PLUGIN_LIFECYCLE_SPAN);
        span.setAttribute(PiTelemetryNames.ATTR_PLUGIN_ID, redactor.safeTag(pluginId));
        span.setAttribute("pi.plugin.action", redactor.safeTag(action));
        String status = "success";
        String selectionStatus = "unknown";
        try (Scope ignored = span.makeCurrent()) {
            T result = supplier.get();
            if (result instanceof PluginMutationStatus mutationStatus) {
                status = mutationStatus.status();
                pluginId = mutationStatus.pluginId().isBlank() ? pluginId : mutationStatus.pluginId();
            } else if (result instanceof List<?> list) {
                status = "success";
                selectionStatus = list.stream()
                        .filter(PluginSourceStatus.class::isInstance)
                        .map(PluginSourceStatus.class::cast)
                        .map(plugin -> plugin.metadata().getOrDefault("selectionStatus", "selected"))
                        .findFirst()
                        .orElse("none");
                span.setAttribute("pi.plugin.count", list.size());
            }
            return result;
        } catch (RuntimeException ex) {
            status = "error";
            span.recordException(new RuntimeException(ex.getClass().getSimpleName()));
            throw ex;
        } finally {
            List<Tag> tags = List.of(
                    Tag.of("plugin_id", redactor.safeTag(pluginId)),
                    Tag.of("action", redactor.safeTag(action)),
                    Tag.of("status", redactor.safeTag(status)),
                    Tag.of("selection_status", redactor.safeTag(selectionStatus)));
            telemetry.counter(PiTelemetryNames.PLUGIN_LIFECYCLE_TOTAL, tags).increment();
            if (discoveryTimer) {
                sample.stop(Timer.builder(PiTelemetryNames.PLUGIN_DISCOVERY_DURATION).tags(tags).register(telemetry.meterRegistry()));
            }
            span.setAttribute(PiTelemetryNames.ATTR_STATUS, redactor.safeTag(status));
            span.setAttribute("pi.plugin.selection_status", redactor.safeTag(selectionStatus));
            span.end();
        }
    }
}
