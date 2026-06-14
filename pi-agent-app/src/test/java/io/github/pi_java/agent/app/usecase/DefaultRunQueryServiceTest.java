package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.common.PlatformIds.*;
import io.github.pi_java.agent.domain.event.*;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRunQueryServiceTest {

    @Test
    void listEventsDelegatesToEventStoreAndComputesCursor() {
        RecordingRunEventStore events = new RecordingRunEventStore();
        DefaultRunQueryService service = new DefaultRunQueryService(new RecordingRunProjectionRepository(), events);

        EventHistoryResponse response = service.listEvents(context(), "session-1", "run-1", 4, 10);

        assertThat(events.listRunId).isEqualTo("run-1");
        assertThat(events.afterSequence).isEqualTo(4);
        assertThat(events.limit).isEqualTo(10);
        assertThat(response.afterSequence()).isEqualTo(4);
        assertThat(response.nextAfterSequence()).isEqualTo(6);
        assertThat(response.terminal()).isTrue();
        assertThat(response.events()).extracting("type").containsExactly("run.created", "run.completed");
    }

    @Test
    void queryMethodsDelegateToProjectionRepository() {
        RecordingRunProjectionRepository projections = new RecordingRunProjectionRepository();
        DefaultRunQueryService service = new DefaultRunQueryService(projections, new RecordingRunEventStore());

        service.getRunDetail(context(), "session-1", "run-1");
        service.getRunStatus(context(), "session-1", "run-1");
        service.listSteps(context(), "session-1", "run-1", 3);
        service.listMessages(context(), "session-1", "run-1", 4);
        service.listToolCalls(context(), "session-1", "run-1", 5);
        service.getRunResult(context(), "session-1", "run-1");

        assertThat(projections.calls).containsExactly("detail", "status", "steps:3", "messages:4", "toolCalls:5", "result");
    }

    static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    static RunEvent event(long sequence, RunEventType type, RunStatus status) {
        return new RunEvent("event-" + sequence, new TenantId("tenant-1"), new UserId("user-1"),
                new SessionId("session-1"), new RunId("run-1"), new StepId("step-none"),
                new WorkspaceId("workspace-1"), sequence, Instant.parse("2026-06-14T05:00:00Z"), type,
                new TraceId("trace-1"), new CorrelationId("correlation-1"), new CausationId("causation-1"),
                new RunEventPayload.RunLifecyclePayload(status, null), EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }

    private static final class RecordingRunEventStore implements RunEventStore {
        String listRunId;
        long afterSequence;
        int limit;

        @Override public void append(RunEvent event) { }
        @Override public List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
            this.listRunId = runId; this.afterSequence = afterSequence; this.limit = limit;
            return List.of(event(5, RunEventType.RUN_CREATED, RunStatus.QUEUED), event(6, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED));
        }
        @Override public Optional<RunEvent> findLastByRun(String runId) { return Optional.empty(); }
        @Override public boolean hasTerminalEvent(String runId) { return true; }
    }

    private static final class RecordingRunProjectionRepository implements RunProjectionRepository {
        final java.util.ArrayList<String> calls = new java.util.ArrayList<>();
        @Override public void createRun(RequestContext context, String sessionId, String runId, io.github.pi_java.agent.client.run.CreateRunRequest request) { }
        @Override public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return Optional.empty(); }
        @Override public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { calls.add("status"); return new RunStatusResponse(sessionId, runId, "QUEUED", false, Instant.now(), "trace-1", "correlation-1"); }
        @Override public boolean markRunning(String runId, Instant startedAt) { return false; }
        @Override public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return false; }
        @Override public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { return false; }
        @Override public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { calls.add("detail"); return new RunDetailResponse(null, List.of(), List.of(), List.of(), List.of(), null); }
        @Override public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { calls.add("steps:" + limit); return new PageResponse<>(List.of(), limit, null, null, false); }
        @Override public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { calls.add("messages:" + limit); return new PageResponse<>(List.of(), limit, null, null, false); }
        @Override public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { calls.add("toolCalls:" + limit); return new PageResponse<>(List.of(), limit, null, null, false); }
        @Override public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { calls.add("result"); return new RunResultResponse(runId, "QUEUED", Map.of(), Map.of()); }
    }
}
