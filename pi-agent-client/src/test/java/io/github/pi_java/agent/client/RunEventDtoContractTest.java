package io.github.pi_java.agent.client;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RedactionDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunEventDtoContractTest {

    @Test
    void runEventDtoPreservesEnvelopeFields() {
        var redaction = new RedactionDto(true, List.of("payload.secret"), "default-redaction");
        var timestamp = Instant.parse("2026-06-14T05:10:00Z");
        var payload = Map.<String, Object>of(
                "schema", "run.lifecycle",
                "status", "completed"
        );

        var event = new RunEventDto(
                "event-001",
                "tenant-001",
                "user-001",
                "session-001",
                "run-001",
                "step-001",
                "workspace-001",
                42L,
                timestamp,
                "run.completed",
                "trace-001",
                "correlation-001",
                "causation-001",
                "tenant",
                redaction,
                "run.lifecycle",
                1,
                payload
        );

        assertThat(event.eventId()).isEqualTo("event-001");
        assertThat(event.tenantId()).isEqualTo("tenant-001");
        assertThat(event.userId()).isEqualTo("user-001");
        assertThat(event.sessionId()).isEqualTo("session-001");
        assertThat(event.runId()).isEqualTo("run-001");
        assertThat(event.stepId()).isEqualTo("step-001");
        assertThat(event.workspaceId()).isEqualTo("workspace-001");
        assertThat(event.sequence()).isEqualTo(42L);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.type()).isEqualTo("run.completed");
        assertThat(event.traceId()).isEqualTo("trace-001");
        assertThat(event.correlationId()).isEqualTo("correlation-001");
        assertThat(event.causationId()).isEqualTo("causation-001");
        assertThat(event.visibility()).isEqualTo("tenant");
        assertThat(event.redaction()).isEqualTo(redaction);
        assertThat(event.payloadSchema()).isEqualTo("run.lifecycle");
        assertThat(event.payloadVersion()).isEqualTo(1);
        assertThat(event.payload()).containsEntry("status", "completed");

        assertThat(RunEventDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly(
                        "eventId",
                        "tenantId",
                        "userId",
                        "sessionId",
                        "runId",
                        "stepId",
                        "workspaceId",
                        "sequence",
                        "timestamp",
                        "type",
                        "traceId",
                        "correlationId",
                        "causationId",
                        "visibility",
                        "redaction",
                        "payloadSchema",
                        "payloadVersion",
                        "payload"
                );
    }

    @Test
    void eventHistoryResponseUsesRunScopedSequenceCursor() {
        var event = new RunEventDto(
                "event-002",
                "tenant-001",
                "user-001",
                "session-001",
                "run-001",
                "step-001",
                "workspace-001",
                7L,
                Instant.parse("2026-06-14T05:11:00Z"),
                "model.delta",
                "trace-001",
                "correlation-001",
                "causation-001",
                "tenant",
                new RedactionDto(false, List.of(), "none"),
                "model.delta",
                1,
                Map.of("textDelta", "hello")
        );

        var response = new EventHistoryResponse(
                "session-001",
                "run-001",
                List.of(event),
                6L,
                7L,
                false
        );

        assertThat(response.sessionId()).isEqualTo("session-001");
        assertThat(response.runId()).isEqualTo("run-001");
        assertThat(response.events()).containsExactly(event);
        assertThat(response.afterSequence()).isEqualTo(6L);
        assertThat(response.nextAfterSequence()).isEqualTo(7L);
        assertThat(response.terminal()).isFalse();
    }

    @Test
    void eventDtosDoNotImportDomainTypes() throws Exception {
        var dtoSources = List.of(
                Path.of("src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/event/RedactionDto.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/event/EventHistoryResponse.java")
        );

        for (Path source : dtoSources) {
            assertThat(Files.readString(source))
                    .as(source.toString())
                    .doesNotContain("io.github.pi_java.agent.domain");
        }
    }
}
