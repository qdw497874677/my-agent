package io.github.pi_java.agent.domain.event;

import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RunEventContractTest {

    @Test
    void run_event_envelope_requires_context_sequence_payload_visibility_and_redaction() {
        RedactionMetadata redaction = new RedactionMetadata(true, true, Set.of("payload.secret"), "redact.default");
        RunEvent event = new RunEvent(
                "event-1",
                new TenantId("tenant-1"),
                new UserId("user-1"),
                new SessionId("session-1"),
                new RunId("run-1"),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                1,
                Instant.parse("2026-06-13T00:00:00Z"),
                RunEventType.RUN_CREATED,
                new TraceId("trace-1"),
                new CorrelationId("correlation-1"),
                new CausationId("causation-1"),
                new RunEventPayload.RunLifecyclePayload(RunStatus.QUEUED, null),
                EventVisibility.ADMIN,
                redaction);

        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.tenantId()).isEqualTo(new TenantId("tenant-1"));
        assertThat(event.stepId()).isEqualTo(new StepId("step-1"));
        assertThat(event.sequence()).isEqualTo(1);
        assertThat(event.payload()).isInstanceOf(RunEventPayload.RunLifecyclePayload.class);
        assertThat(event.visibility()).isEqualTo(EventVisibility.ADMIN);
        assertThat(event.redaction()).isEqualTo(redaction);

        assertThatNullPointerException().isThrownBy(() -> new RunEvent(
                "event-2", null, new UserId("user-1"), new SessionId("session-1"), new RunId("run-1"),
                new StepId("step-1"), new WorkspaceId("workspace-1"), 0, Instant.now(), RunEventType.RUN_CREATED,
                new TraceId("trace-1"), new CorrelationId("correlation-1"), new CausationId("causation-1"),
                new RunEventPayload.ExtensionPayload("x", "1", Map.of()), EventVisibility.INTERNAL, redaction));
    }

    @Test
    void event_taxonomy_reserves_core_families_with_stable_wire_names() {
        assertThat(RunEventType.RUN_CREATED.wireName()).isEqualTo("run.created");
        assertThat(RunEventType.TOOL_COMPLETED.wireName()).isEqualTo("tool.completed");

        Set<String> families = Arrays.stream(RunEventType.values())
                .map(RunEventType::wireName)
                .map(name -> name.substring(0, name.indexOf('.')))
                .collect(Collectors.toSet());

        assertThat(families).contains("run", "step", "model", "tool", "policy", "workspace", "artifact", "message");
    }
}
