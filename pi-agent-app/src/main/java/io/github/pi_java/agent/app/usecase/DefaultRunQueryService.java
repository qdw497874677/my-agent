package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RedactionDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultRunQueryService implements RunQueryService {

    private final RunProjectionRepository runProjectionRepository;
    private final RunEventStore runEventStore;

    public DefaultRunQueryService(RunProjectionRepository runProjectionRepository, RunEventStore runEventStore) {
        this.runProjectionRepository = Objects.requireNonNull(runProjectionRepository, "runProjectionRepository must not be null");
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
    }

    @Override
    public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getRunDetail(context, sessionId, runId);
    }

    @Override
    public RunStatusResponse getRunStatus(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getStatus(context, sessionId, runId);
    }

    @Override
    public EventHistoryResponse listEvents(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
        List<RunEventDto> events = runEventStore.listByRun(runId, afterSequence, limit).stream()
                .map(DefaultRunQueryService::toDto)
                .toList();
        long nextAfterSequence = events.stream()
                .mapToLong(RunEventDto::sequence)
                .max()
                .orElse(afterSequence);
        return new EventHistoryResponse(sessionId, runId, events, afterSequence, nextAfterSequence,
                runEventStore.hasTerminalEvent(runId));
    }

    @Override
    public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listSteps(context, sessionId, runId, limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listMessages(context, sessionId, runId, limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listToolCalls(context, sessionId, runId, limit);
    }

    @Override
    public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getRunResult(context, sessionId, runId);
    }

    private static RunEventDto toDto(RunEvent event) {
        return new RunEventDto(
                event.eventId(), event.tenantId().value(), event.userId().value(), event.sessionId().value(),
                event.runId().value(), event.stepId().value(), event.workspaceId().value(), event.sequence(),
                event.timestamp(), event.type().wireName(), event.traceId().value(), event.correlationId().value(),
                event.causationId().value(), event.visibility().name(), toDto(event.redaction()), payloadSchema(event.payload()),
                1, payloadAttributes(event.payload()));
    }

    private static RedactionDto toDto(RedactionMetadata redaction) {
        return new RedactionDto(redaction.redacted(), List.copyOf(redaction.redactedFields()), redaction.policyRef());
    }

    private static String payloadSchema(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.schema();
        }
        return payload.getClass().getSimpleName();
    }

    private static Map<String, Object> payloadAttributes(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.RunLifecyclePayload runLifecyclePayload) {
            return runLifecyclePayload.failureSummary() == null
                    ? Map.of("status", runLifecyclePayload.status().name())
                    : Map.of("status", runLifecyclePayload.status().name(), "failureSummary", runLifecyclePayload.failureSummary().message());
        }
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.attributes();
        }
        return Map.of("kind", payload.getClass().getSimpleName());
    }
}
