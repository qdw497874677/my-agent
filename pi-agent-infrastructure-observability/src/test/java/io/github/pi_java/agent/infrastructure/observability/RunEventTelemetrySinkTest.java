package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunEventTelemetrySinkTest {

    private static final String FAKE_SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void terminal_run_event_records_safe_event_metric_and_delegates_once() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RecordingEventSink delegate = new RecordingEventSink();
        RunEventTelemetrySink sink = new RunEventTelemetrySink(delegate, new PiTelemetry(registry, null));
        RunEvent event = sampleEvent(RunEventType.RUN_COMPLETED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.SUCCEEDED, null));

        sink.publish(event);

        assertThat(delegate.events).containsExactly(event);
        assertThat(registry.get(PiTelemetryNames.RUN_EVENTS_TOTAL)
                .tags("event_type", "run.completed", "terminal", "true", "source", "run_event", "status", "completed")
                .counter().count()).isEqualTo(1.0d);
    }

    @Test
    void model_and_tool_payload_values_do_not_become_meter_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RunEventTelemetrySink sink = new RunEventTelemetrySink(event -> { }, new PiTelemetry(registry, null));

        sink.publish(sampleEvent(RunEventType.MODEL_DELTA,
                new RunEventPayload.ModelDeltaPayload("provider:model", FAKE_SECRET)));
        sink.publish(sampleEvent(RunEventType.TOOL_UPDATED,
                new RunEventPayload.ExtensionPayload("tool.lifecycle", "v1", Map.of("token", FAKE_SECRET))));

        assertThat(registry.getMeters()).hasSize(2);
        assertThat(allMeterIdText(registry)).contains("model.delta").contains("tool.updated")
                .doesNotContain(FAKE_SECRET)
                .doesNotContain("token");
    }

    @Test
    void delegate_exception_is_preserved_after_telemetry() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        IllegalStateException failure = new IllegalStateException("boom");
        RunEventTelemetrySink sink = new RunEventTelemetrySink(event -> { throw failure; }, new PiTelemetry(registry, null));

        assertThatThrownBy(() -> sink.publish(sampleEvent(RunEventType.RUN_FAILED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.FAILED, null))))
                .isSameAs(failure);

        assertThat(registry.get(PiTelemetryNames.RUN_EVENTS_TOTAL)
                .tags("event_type", "run.failed", "status", "failed")
                .counter().count()).isEqualTo(1.0d);
    }

    private static String allMeterIdText(SimpleMeterRegistry registry) {
        return registry.getMeters().stream()
                .map(Meter::getId)
                .map(Meter.Id::toString)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private static RunEvent sampleEvent(RunEventType type, RunEventPayload payload) {
        return new RunEvent(
                "event-" + type.wireName(),
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-a"),
                new RunId("run-a"),
                new StepId("step-a"),
                new WorkspaceId("workspace-a"),
                1,
                Instant.parse("2026-06-19T00:00:00Z"),
                type,
                new TraceId("4bf92f3577b34da6a3ce929d0e0e4736"),
                new CorrelationId("corr-a"),
                new CausationId("cause-a"),
                payload,
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }

    private static final class RecordingEventSink implements EventSink {
        private final List<RunEvent> events = new ArrayList<>();

        @Override
        public void publish(RunEvent event) {
            events.add(event);
        }
    }
}
