package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.usecase.ConversationContextAssembler;
import io.github.pi_java.agent.app.usecase.ConversationContextPolicy;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRunDispatcherContextTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void dispatchInjectsAssemblerMessagesBeforeRuntimeAndKeepsCurrentPromptOnlyInRunInput() {
        RecordingConversationQueryService conversations = new RecordingConversationQueryService(List.of(
                message("msg-1", "prior-run-1", ConversationMessageRole.USER, "first prior question"),
                message("msg-2", "prior-run-1", ConversationMessageRole.ASSISTANT, "first prior answer"),
                message("msg-current", "run-1", ConversationMessageRole.USER, "current prompt should stay current")
        ));
        Fixture fixture = new Fixture(conversations);

        fixture.dispatcher.dispatch("worker-context");

        RunContext context = fixture.runtime.startedContext.get();
        assertThat(context).isNotNull();
        assertThat(conversations.context.get()).isNotNull();
        assertThat(conversations.sessionId.get()).isEqualTo("session-1");
        assertThat(conversations.limit.get()).isEqualTo(48);
        assertThat(context.input()).isInstanceOf(RunInput.ChatInput.class);
        assertThat(((RunInput.ChatInput) context.input()).text()).isEqualTo("current prompt should stay current");
        assertThat(context.sessionContext().messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("first prior question", "first prior answer")
                .doesNotContain("current prompt should stay current");
        assertThat(context.sessionContext().messages())
                .extracting(SessionEntryPayload.MessageEntry::role)
                .containsExactly("user", "assistant");
    }

    @Test
    void emptyAssemblerHistoryStillCreatesValidEmptySessionContext() {
        Fixture fixture = new Fixture(new RecordingConversationQueryService(List.of()));

        fixture.dispatcher.dispatch("worker-context");

        RunContext context = fixture.runtime.startedContext.get();
        assertThat(context).isNotNull();
        assertThat(context.sessionContext().messages()).isEmpty();
        assertThat(context.sessionContext().workspaceScope()).isPresent();
    }

    private static ConversationMessageDto message(String id, String runId, ConversationMessageRole role, String text) {
        return new ConversationMessageDto(
                id,
                "session-1",
                runId,
                null,
                role,
                text,
                ConversationMessageStatus.COMPLETED,
                CLOCK.instant(),
                CLOCK.instant(),
                1L,
                1L,
                Map.of(),
                true,
                false);
    }

    private static final class Fixture {
        private final FakeRunQueue queue = new FakeRunQueue();
        private final FakeProjectionRepository projections = new FakeProjectionRepository();
        private final FakeRunEventStore eventStore = new FakeRunEventStore();
        private final FakeTerminalPublisher publisher = new FakeTerminalPublisher(eventStore);
        private final InMemoryCancellationRegistry registry = new InMemoryCancellationRegistry();
        private final RecordingAuditRepository audit = new RecordingAuditRepository();
        private final RecordingRuntime runtime = new RecordingRuntime();
        private final DefaultRunDispatcher dispatcher;

        private Fixture(ConversationQueryService conversations) {
            ConversationContextPolicy policy = ConversationContextPolicy.defaults();
            dispatcher = new DefaultRunDispatcher(
                    queue,
                    projections,
                    eventStore,
                    publisher,
                    registry,
                    audit,
                    runtime,
                    CLOCK,
                    Duration.ofSeconds(5),
                    "openai-compatible:gpt-test",
                    new ConversationContextAssembler(conversations, policy),
                    policy);
        }
    }

    private static final class RecordingConversationQueryService implements ConversationQueryService {
        private final List<ConversationMessageDto> messages;
        private final AtomicReference<RequestContext> context = new AtomicReference<>();
        private final AtomicReference<String> sessionId = new AtomicReference<>();
        private final AtomicReference<Integer> limit = new AtomicReference<>();

        private RecordingConversationQueryService(List<ConversationMessageDto> messages) {
            this.messages = List.copyOf(messages);
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
            this.context.set(context);
            this.sessionId.set(sessionId);
            this.limit.set(limit);
            return new ConversationTranscriptResponse(sessionId, messages, null, null, null, false, Map.of());
        }
    }

    private static final class RecordingRuntime implements AgentRuntime {
        private final AtomicReference<RunContext> startedContext = new AtomicReference<>();

        @Override
        public RunHandle start(RunContext context) {
            startedContext.set(context);
            return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
        }

        @Override
        public void cancel(String runId, String reason) {
        }
    }

    private static final class FakeRunQueue implements RunQueue {
        private final QueuedRun run = new QueuedRun("run-1", "session-1", "tenant-1", "user-1", "workspace-1", "trace-1", "correlation-1", "chat", Map.of("text", "current prompt should stay current"), CLOCK.instant(), 0);
        private boolean claimed;

        @Override public void enqueue(QueuedRun run) { }
        @Override public Optional<QueuedRun> claimNext(String workerId, Instant now) { if (claimed) return Optional.empty(); claimed = true; return Optional.of(run); }
        @Override public boolean markRunning(String runId, Instant startedAt) { return true; }
        @Override public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { return true; }
        @Override public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return Optional.empty(); }
        @Override public boolean removeIfTerminal(String runId) { return false; }
    }

    private static final class FakeProjectionRepository implements RunProjectionRepository {
        @Override public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { }
        @Override public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return Optional.empty(); }
        @Override public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return null; }
        @Override public boolean markRunning(String runId, Instant startedAt) { return true; }
        @Override public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return true; }
        @Override public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { return true; }
        @Override public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return null; }
        @Override public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return null; }
    }

    private static final class FakeRunEventStore implements RunEventStore {
        private final AtomicBoolean terminal = new AtomicBoolean();
        @Override public void append(RunEvent event) { terminal.set(true); }
        @Override public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return List.of(); }
        @Override public Optional<RunEvent> findLastByRun(String runId) { return Optional.empty(); }
        @Override public boolean hasTerminalEvent(String runId) { return terminal.get(); }
    }

    private static final class FakeTerminalPublisher implements RunTerminalEventPublisher {
        private final FakeRunEventStore store;
        private FakeTerminalPublisher(FakeRunEventStore store) { this.store = store; }
        @Override public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) { store.terminal.set(true); return true; }
        @Override public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) { store.terminal.set(true); return true; }
        @Override public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) { store.terminal.set(true); return true; }
        @Override public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) { store.terminal.set(true); return true; }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        private final List<String> actions = new ArrayList<>();
        @Override public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) { actions.add(action); }
    }
}
