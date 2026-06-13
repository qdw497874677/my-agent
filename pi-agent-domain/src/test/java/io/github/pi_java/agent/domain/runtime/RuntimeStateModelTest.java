package io.github.pi_java.agent.domain.runtime;

import io.github.pi_java.agent.domain.error.FailureSummary;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.error.PiError.Category;
import io.github.pi_java.agent.domain.error.PiError.Severity;
import io.github.pi_java.agent.domain.event.EventVisibility;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeStateModelTest {

    @Test
    void run_status_contains_governance_and_cancellation_semantics() {
        assertThat(Arrays.asList(RunStatus.values()))
                .contains(RunStatus.QUEUED, RunStatus.RUNNING, RunStatus.SUSPENDED, RunStatus.CANCELLING,
                        RunStatus.SUCCEEDED, RunStatus.FAILED, RunStatus.CANCELLED, RunStatus.POLICY_BLOCKED);
        assertThat(Arrays.asList(StepStatus.values()))
                .contains(StepStatus.PENDING, StepStatus.RUNNING, StepStatus.SUSPENDED, StepStatus.SUCCEEDED,
                        StepStatus.FAILED, StepStatus.CANCELLED, StepStatus.SKIPPED);
    }

    @Test
    void failure_summary_exposes_normalized_safe_error_flags() {
        PiError error = new PiError(
                Category.TIMEOUT,
                "MODEL_TIMEOUT",
                Severity.ERROR,
                EventVisibility.USER,
                true,
                true,
                false);
        FailureSummary summary = new FailureSummary("Model call timed out after the configured deadline.", error);

        assertThat(summary.message()).contains("timed out");
        assertThat(summary.error().category()).isEqualTo(Category.TIMEOUT);
        assertThat(summary.error().code()).isEqualTo("MODEL_TIMEOUT");
        assertThat(summary.error().severity()).isEqualTo(Severity.ERROR);
        assertThat(summary.error().visibility()).isEqualTo(EventVisibility.USER);
        assertThat(summary.error().retryable()).isTrue();
        assertThat(summary.error().recoverable()).isTrue();
        assertThat(summary.error().userActionRequired()).isFalse();
    }
}
