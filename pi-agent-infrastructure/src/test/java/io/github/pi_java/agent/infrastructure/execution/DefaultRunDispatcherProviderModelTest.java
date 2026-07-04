package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRunDispatcherProviderModelTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void dispatcherUsesQueuedRunModelSnapshotInsteadOfConstructorDefault() {
        Fixture fixture = new Fixture(new RunProviderMetadata(
                "openai-compatible:requested", "openai-compatible:selected", "openai-compatible", "selected", null, "READY", null));

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.runtime.startedContext.get().agentDefinition().modelRef()).isEqualTo("openai-compatible:selected");
        assertThat(fixture.projections.terminalStatus).isEqualTo("COMPLETED");
    }

    @Test
    void dispatcherFallsBackToConstructorDefaultWhenQueuedRunHasNoSnapshot() {
        Fixture fixture = new Fixture(RunProviderMetadata.EMPTY);

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.runtime.startedContext.get().agentDefinition().modelRef()).isEqualTo("openai-compatible:constructor-default");
        assertThat(fixture.projections.terminalStatus).isEqualTo("COMPLETED");
    }

    @Test
    void invalidQueuedSnapshotFailsBeforeRuntimeStartWithoutSecrets() {
        Fixture fixture = new Fixture(new RunProviderMetadata(null, "bad-model", "openai-compatible", "bad-model", null, "READY", "Bearer secret sk-live"));

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.runtime.startedContext.get()).isNull();
        assertThat(fixture.projections.terminalStatus).isEqualTo("FAILED");
        assertThat(fixture.projections.failure).containsEntry("errorType", "IllegalArgumentException");
        assertThat(fixture.projections.failure.toString()).doesNotContain("secret", "sk-live");
    }

    private static final class Fixture {
        private final FakeRunQueue queue;
        private final FakeProjectionRepository projections = new FakeProjectionRepository();
        private final FakeRunEventStore eventStore = new FakeRunEventStore();
        private final FakeTerminalPublisher publisher = new FakeTerminalPublisher(eventStore);
        private final InMemoryCancellationRegistry registry = new InMemoryCancellationRegistry();
        private final FakeAuditRepository audit = new FakeAuditRepository();
        private final FakeRuntime runtime = new FakeRuntime();
        private final DefaultRunDispatcher dispatcher;

        private Fixture(RunProviderMetadata metadata) {
            queue = new FakeRunQueue(metadata);
            dispatcher = new DefaultRunDispatcher(queue, projections, eventStore, publisher, registry, audit,
                    runtime, CLOCK, Duration.ofSeconds(5), "openai-compatible:constructor-default");
        }
    }

    private static final class FakeRuntime implements AgentRuntime {
        private final AtomicReference<RunContext> startedContext = new AtomicReference<>();
        @Override public RunHandle start(RunContext context) { startedContext.set(context); return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty()); }
        @Override public void cancel(String runId, String reason) { }
    }

    private static final class FakeRunQueue implements RunQueue {
        private final QueuedRun run;
        private boolean claimed;
        private FakeRunQueue(RunProviderMetadata metadata) {
            run = new QueuedRun("run-1", "session-1", "tenant-1", "user-1", "workspace-1", "trace-1", "correlation-1",
                    "chat", Map.of("text", "hello"), metadata, CLOCK.instant(), 0);
        }
        @Override public void enqueue(QueuedRun run) { }
        @Override public Optional<QueuedRun> claimNext(String workerId, Instant now) { if (claimed) return Optional.empty(); claimed = true; return Optional.of(run); }
        @Override public boolean markRunning(String runId, Instant startedAt) { return true; }
        @Override public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { return true; }
        @Override public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return Optional.empty(); }
        @Override public boolean removeIfTerminal(String runId) { return false; }
    }

    private static final class FakeProjectionRepository implements RunProjectionRepository {
        String terminalStatus;
        Map<String, Object> failure = Map.of();
        @Override public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { }
        @Override public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return Optional.empty(); }
        @Override public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return null; }
        @Override public boolean markRunning(String runId, Instant startedAt) { return true; }
        @Override public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return true; }
        @Override public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { this.terminalStatus = status; this.failure = failure == null ? Map.of() : failure; return true; }
        @Override public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return null; }
        @Override public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return null; }
    }

    private static final class FakeRunEventStore implements RunEventStore {
        @Override public void append(RunEvent event) { }
        @Override public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return List.of(); }
        @Override public Optional<RunEvent> findLastByRun(String runId) { return Optional.empty(); }
        @Override public boolean hasTerminalEvent(String runId) { return false; }
    }

    private static final class FakeTerminalPublisher implements RunTerminalEventPublisher {
        @SuppressWarnings("unused") private final FakeRunEventStore store;
        private FakeTerminalPublisher(FakeRunEventStore store) { this.store = store; }
        @Override public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) { return true; }
        @Override public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) { return true; }
        @Override public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) { return true; }
        @Override public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) { return true; }
    }

    private static final class FakeAuditRepository implements AuditRepository {
        @Override public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) { }
    }
}
