package io.github.pi_java.agent.adapter.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunEventDtoMapperTest {

    @Test
    void mapsModelDeltaPayloadWithoutNullableFinishFields() {
        RunEvent event = event(new RunEventPayload.ModelDeltaPayload(
                "openai-compatible:gpt-fake-e2e",
                "fake-openai ",
                "openai-compatible",
                "gpt-fake-e2e",
                null,
                null,
                Duration.ofMillis(12)));

        RunEventDto dto = RunEventDtoMapper.toDto(event);

        assertThat(dto.payload()).containsEntry("textDelta", "fake-openai ");
        assertThat(dto.payload()).containsEntry("modelRef", "openai-compatible:gpt-fake-e2e");
        assertThat(dto.payload()).containsEntry("providerId", "openai-compatible");
        assertThat(dto.payload()).doesNotContainKeys("finishReason", "usage");
    }

    private static RunEvent event(RunEventPayload payload) {
        return new RunEvent(
                "evt-model-delta",
                new TenantId("tenant-test"),
                new UserId("user-test"),
                new SessionId("session-test"),
                new RunId("run-test"),
                new StepId("model-step"),
                new WorkspaceId("workspace-test"),
                1L,
                Instant.parse("2026-07-05T00:00:00Z"),
                RunEventType.MODEL_DELTA,
                new TraceId("11111111111111111111111111111111"),
                new CorrelationId("correlation-test"),
                new CausationId("model-stream"),
                payload,
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "provider-runtime"));
    }
}
