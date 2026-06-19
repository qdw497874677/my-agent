package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryRunDispatcherTest {

    @Test
    void dispatch_records_success_duration_with_safe_worker_tag_and_cleans_mdc() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RecordingDispatcher delegate = new RecordingDispatcher();
        TelemetryRunDispatcher dispatcher = new TelemetryRunDispatcher(delegate, new PiTelemetry(registry, null));

        dispatcher.dispatch("worker-a");

        assertThat(delegate.dispatchCalls).isEqualTo(1);
        assertThat(registry.get(PiTelemetryNames.RUN_DISPATCH_DURATION)
                .tags("operation", "dispatch", "status", "success", "worker_id", "worker-a")
                .timer().count()).isEqualTo(1);
        assertThat(MDC.get("workerId")).isNull();
    }

    @Test
    void dispatchRun_records_error_status_and_rethrows_delegate_failure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        IllegalStateException failure = new IllegalStateException("boom");
        RunDispatcher delegate = new RunDispatcher() {
            @Override
            public void dispatch(String workerId) {
            }

            @Override
            public void dispatchRun(String workerId, String runId) {
                throw failure;
            }
        };
        TelemetryRunDispatcher dispatcher = new TelemetryRunDispatcher(delegate, new PiTelemetry(registry, null));

        assertThatThrownBy(() -> dispatcher.dispatchRun("worker-a", "run-a"))
                .isSameAs(failure);

        assertThat(registry.get(PiTelemetryNames.RUN_DISPATCH_DURATION)
                .tags("operation", "dispatchRun", "status", "error", "worker_id", "worker-a", "run_id", "run-a")
                .timer().count()).isEqualTo(1);
    }

    @Test
    void sensitive_worker_or_run_values_are_redacted_from_meter_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryRunDispatcher dispatcher = new TelemetryRunDispatcher(new RecordingDispatcher(), new PiTelemetry(registry, null));

        dispatcher.dispatchRun("worker-secret-token", "run-api_key-value");

        String meterIds = registry.getMeters().stream()
                .map(Meter::getId)
                .map(Meter.Id::toString)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(meterIds)
                .contains(PiTelemetryRedactor.REDACTED)
                .doesNotContain("worker-secret-token")
                .doesNotContain("run-api_key-value");
    }

    private static final class RecordingDispatcher implements RunDispatcher {
        private int dispatchCalls;

        @Override
        public void dispatch(String workerId) {
            dispatchCalls++;
        }

        @Override
        public void dispatchRun(String workerId, String runId) {
            dispatchCalls++;
        }
    }
}
