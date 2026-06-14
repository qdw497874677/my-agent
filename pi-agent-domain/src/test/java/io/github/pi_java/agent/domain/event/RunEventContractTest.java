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
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
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
        assertThat(event.redaction().containsSecrets()).isTrue();
        assertThat(event.redaction().redacted()).isTrue();
        assertThat(event.redaction().redactedFields()).containsExactly("payload.secret");
        assertThat(event.redaction().policyRef()).isEqualTo("redact.default");

        assertThatNullPointerException().isThrownBy(() -> new RunEvent(
                "event-2", null, new UserId("user-1"), new SessionId("session-1"), new RunId("run-1"),
                new StepId("step-1"), new WorkspaceId("workspace-1"), 0, Instant.now(), RunEventType.RUN_CREATED,
                new TraceId("trace-1"), new CorrelationId("correlation-1"), new CausationId("causation-1"),
                new RunEventPayload.ExtensionPayload("x", "1", Map.of()), EventVisibility.INTERNAL, redaction));
    }

    @Test
    void event_taxonomy_reserves_core_families_with_stable_wire_names() {
        assertThat(RunEventType.RUN_CREATED.wireName()).isEqualTo("run.created");
        assertThat(RunEventType.RUN_STARTED.wireName()).isEqualTo("run.started");
        assertThat(RunEventType.RUN_COMPLETED.wireName()).isEqualTo("run.completed");
        assertThat(RunEventType.RUN_FAILED.wireName()).isEqualTo("run.failed");
        assertThat(RunEventType.RUN_CANCELLED.wireName()).isEqualTo("run.cancelled");
        assertThat(RunEventType.RUN_POLICY_BLOCKED.wireName()).isEqualTo("run.policy_blocked");
        assertThat(RunEventType.MODEL_REQUESTED.wireName()).isEqualTo("model.requested");
        assertThat(RunEventType.TOOL_COMPLETED.wireName()).isEqualTo("tool.completed");

        Set<String> families = Arrays.stream(RunEventType.values())
                .map(RunEventType::wireName)
                .map(name -> name.substring(0, name.indexOf('.')))
                .collect(Collectors.toSet());

        assertThat(families).contains("run", "step", "model", "tool", "policy", "workspace", "artifact", "message");
    }

    @Test
    void extension_payload_path_preserves_schema_version_and_attributes() {
        RunEventPayload.ExtensionPayload payload = new RunEventPayload.ExtensionPayload(
                "adapter.custom-event",
                "1",
                Map.of("provider", "fake", "nested", Map.of("key", "value")));

        assertThat(payload.schema()).isEqualTo("adapter.custom-event");
        assertThat(payload.version()).isEqualTo("1");
        assertThat(payload.attributes()).containsEntry("provider", "fake");
        assertThat(payload.attributes()).containsKey("nested");
    }

    @Test
    void redaction_metadata_presence_is_required_for_every_constructed_event() {
        RedactionMetadata redaction = new RedactionMetadata(false, false, Set.of(), "redact.none");

        RunEvent event = new RunEvent(
                "event-redaction",
                new TenantId("tenant-1"),
                new UserId("user-1"),
                new SessionId("session-1"),
                new RunId("run-1"),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                2,
                Instant.parse("2026-06-13T00:00:01Z"),
                RunEventType.MESSAGE_APPENDED,
                new TraceId("trace-1"),
                new CorrelationId("correlation-1"),
                new CausationId("causation-1"),
                new RunEventPayload.MessageAppendedPayload("message-1", "assistant"),
                EventVisibility.USER,
                redaction);

        assertThat(event.visibility()).isNotNull();
        assertThat(event.redaction()).isNotNull();
        assertThat(event.redaction().redactedFields()).isEmpty();
        assertThat(event.redaction().policyRef()).isEqualTo("redact.none");
    }

    @Test
    void model_delta_payload_preserves_legacy_constructor_and_accepts_normalized_metadata() {
        ModelUsage usage = new ModelUsage(2, 4, 6);
        RunEventPayload.ModelDeltaPayload legacy = new RunEventPayload.ModelDeltaPayload("fake:model", "hi");
        RunEventPayload.ModelDeltaPayload enriched = new RunEventPayload.ModelDeltaPayload(
                "openai-compatible:gpt-4.1-mini",
                "hello",
                "openai-compatible",
                "gpt-4.1-mini",
                ModelFinishReason.STOP,
                usage,
                Duration.ofMillis(75));

        assertThat(legacy.modelRef()).isEqualTo("fake:model");
        assertThat(legacy.textDelta()).isEqualTo("hi");
        assertThat(legacy.providerId()).isNull();
        assertThat(enriched.providerId()).isEqualTo("openai-compatible");
        assertThat(enriched.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(enriched.finishReason()).isEqualTo(ModelFinishReason.STOP);
        assertThat(enriched.usage()).isEqualTo(usage);
        assertThat(enriched.latency()).isEqualTo(Duration.ofMillis(75));
    }
}
