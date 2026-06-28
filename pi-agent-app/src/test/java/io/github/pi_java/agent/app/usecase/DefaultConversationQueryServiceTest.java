package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.common.PlatformIds.*;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * App use-case tests for {@link DefaultConversationQueryService} proving that
 * the ownership {@link RequestContext} is threaded through listRecent /
 * getTranscript (decision D-15), that transcript output is typed (decisions
 * D-05, D-13, D-16), and that missing sessions are refused before any run/event
 * load. Uses hand-rolled fakes only (no Mockito / Infrastructure).
 */
class DefaultConversationQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-28T05:00:00Z");

    @Test
    void listRecentSessionsDelegatesToRepositoryWithContextAndPreservesOrder() {
        RecordingSessionRepository sessions = new RecordingSessionRepository();
        sessions.recent = new PageResponse<>(List.<SessionSummaryDto>of(
                summary("session-2", "latest"),
                summary("session-1", "older")), 10, null, 2L, false);
        DefaultConversationQueryService service = new DefaultConversationQueryService(
                sessions, new StubRunProjectionRepository(), new StubRunEventStore(), new ConversationTranscriptAssembler());

        RequestContext ctx = context();
        PageResponse<SessionSummaryDto> page = service.listRecentSessions(ctx, 10, "cursor-1");

        assertThat(sessions.recentContext).isSameAs(ctx);
        assertThat(sessions.recentLimit).isEqualTo(10);
        assertThat(sessions.recentCursor).isEqualTo("cursor-1");
        assertThat(page.items()).extracting(SessionSummaryDto::sessionId)
                .containsExactly("session-2", "session-1");
    }

    @Test
    void getTranscriptRefusesMissingSessionBeforeLoadingRunsOrEvents() {
        RecordingSessionRepository sessions = new RecordingSessionRepository();
        sessions.found = Optional.empty();
        StubRunProjectionRepository runs = new StubRunProjectionRepository();
        StubRunEventStore events = new StubRunEventStore();
        DefaultConversationQueryService service = new DefaultConversationQueryService(
                sessions, runs, events, new ConversationTranscriptAssembler());

        assertThatThrownBy(() -> service.getTranscript(context(), "missing-session", 10, null))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("missing-session");

        assertThat(runs.listRunsCalls).isEmpty();
        assertThat(events.listBySessionRunCalls).isEmpty();
    }

    @Test
    void getTranscriptReturnsTypedConversationTranscriptResponseAndMessages() {
        RecordingSessionRepository sessions = new RecordingSessionRepository();
        StubRunProjectionRepository runs = new StubRunProjectionRepository();
        runs.runsForSession = List.of(
                new ConversationRunView("run-1", NOW, Map.of("text", "hello"), "SUCCEEDED"));
        StubRunEventStore events = new StubRunEventStore();
        events.eventsByRun.put("run-1", List.of(
                delta("run-1", 1, "Hel"),
                delta("run-1", 2, "lo"),
                runLifecycle("run-1", 3, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED, null)));
        DefaultConversationQueryService service = new DefaultConversationQueryService(
                sessions, runs, events, new ConversationTranscriptAssembler());

        RequestContext ctx = context();
        ConversationTranscriptResponse transcript = service.getTranscript(ctx, "session-1", 10, null);

        assertThat(transcript).isInstanceOf(ConversationTranscriptResponse.class);
        assertThat(transcript.sessionId()).isEqualTo("session-1");
        assertThat(transcript.messages()).isNotEmpty();
        assertThat(transcript.messages()).allSatisfy(m -> assertThat(m).isInstanceOf(ConversationMessageDto.class));
        assertThat(transcript.messages()).extracting(ConversationMessageDto::role)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);
        // The new transcript contract must not surface raw-map SessionHistoryResponse entries (D-13).
        assertThat(transcript.messages().get(0).text()).isEqualTo("hello");
        assertThat(transcript.messages().get(1).text()).isEqualTo("Hello");

        // Ownership context is threaded into both the run list and event reads (D-15).
        assertThat(runs.listRunsContext).isSameAs(ctx);
        assertThat(events.listBySessionRunContext).isSameAs(ctx);
        assertThat(events.listBySessionRunCalls).containsExactly("run-1");
    }

    private static RequestContext context() {
        return new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static SessionSummaryDto summary(String sessionId, String preview) {
        return new SessionSummaryDto(sessionId, sessionId, "idle", preview, NOW, NOW, null, null, Map.of());
    }

    private static RunEvent delta(String runId, long sequence, String textDelta) {
        return event(runId, sequence, RunEventType.MODEL_DELTA, "step-" + sequence,
                new RunEventPayload.ModelDeltaPayload("model-ref-1", textDelta));
    }

    private static RunEvent runLifecycle(String runId, long sequence, RunEventType type, RunStatus status,
                                         io.github.pi_java.agent.domain.error.FailureSummary failure) {
        return event(runId, sequence, type, "step-none",
                new RunEventPayload.RunLifecyclePayload(status, failure));
    }

    private static RunEvent event(String runId, long sequence, RunEventType type, String stepId,
                                  RunEventPayload payload) {
        return new RunEvent(
                "event-" + runId + "-" + sequence,
                new TenantId("tenant-1"), new UserId("user-1"), new SessionId("session-1"),
                new RunId(runId), new StepId(stepId), new WorkspaceId("workspace-1"),
                sequence, NOW.plusSeconds(sequence), type,
                new TraceId("0123456789abcdef0123456789abcdef"), new CorrelationId("correlation-1"),
                new CausationId("causation-1"),
                payload, EventVisibility.USER, new RedactionMetadata(false, false, Set.of(), "default"));
    }

    private static final class RecordingSessionRepository implements SessionRepository {
        RequestContext recentContext;
        int recentLimit;
        String recentCursor;
        PageResponse<SessionSummaryDto> recent = new PageResponse<>(List.of(), 10, null, null, false);
        Optional<SessionResponse> found = Optional.of(new SessionResponse(
                "tenant-1", "user-1", "session-1", "workspace-1", null, "ACTIVE", NOW, NOW, Map.of()));

        @Override
        public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) {
            return null;
        }

        @Override
        public Optional<SessionResponse> findById(RequestContext context, String sessionId) {
            return found;
        }

        @Override
        public SessionHistoryResponse history(RequestContext context, String sessionId) {
            return new SessionHistoryResponse(found.orElse(null), List.of());
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecent(RequestContext context, int limit, String cursor) {
            this.recentContext = context;
            this.recentLimit = limit;
            this.recentCursor = cursor;
            return recent;
        }
    }

    private static class StubRunProjectionRepository implements RunProjectionRepository {
        List<ConversationRunView> runsForSession = List.of();
        RequestContext listRunsContext;
        String listRunsSessionId;

        List<String> listRunsCalls = new ArrayList<>();

        @Override
        public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) {
        }

        @Override
        public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) {
            return Optional.empty();
        }

        @Override
        public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) {
            return new RunStatusResponse(sessionId, runId, "QUEUED", false, NOW, "trace-1", "correlation-1");
        }

        @Override
        public boolean markRunning(String runId, Instant startedAt) {
            return false;
        }

        @Override
        public boolean requestCancellation(String runId, String reason, Instant requestedAt) {
            return false;
        }

        @Override
        public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult,
                                                  Map<String, Object> failure, Instant finishedAt) {
            return false;
        }

        @Override
        public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
            return new RunDetailResponse(null, List.of(), List.of(), List.of(), List.of(), null);
        }

        @Override
        public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
            return new RunResultResponse(runId, "QUEUED", Map.of(), Map.of());
        }

        @Override
        public PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor) {
            this.listRunsContext = context;
            this.listRunsSessionId = sessionId;
            this.listRunsCalls.add(sessionId);
            return new PageResponse<>(runsForSession, limit, null, null, false);
        }
    }

    private static class StubRunEventStore implements RunEventStore {
        Map<String, List<RunEvent>> eventsByRun = new java.util.HashMap<>();
        RequestContext listBySessionRunContext;
        String listBySessionRunSessionId;
        List<String> listBySessionRunCalls = new ArrayList<>();

        @Override
        public void append(RunEvent event) {
        }

        @Override
        public List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
            return eventsByRun.getOrDefault(runId, List.of());
        }

        @Override
        public Optional<RunEvent> findLastByRun(String runId) {
            return Optional.empty();
        }

        @Override
        public boolean hasTerminalEvent(String runId) {
            return false;
        }

        @Override
        public List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
            this.listBySessionRunContext = context;
            this.listBySessionRunSessionId = sessionId;
            this.listBySessionRunCalls.add(runId);
            return eventsByRun.getOrDefault(runId, List.of());
        }
    }
}
