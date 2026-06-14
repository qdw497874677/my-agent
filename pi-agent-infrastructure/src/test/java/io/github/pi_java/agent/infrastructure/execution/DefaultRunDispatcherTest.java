package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRunDispatcherTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void successfulRunClaimsMarksRunningStartsRuntimePublishesTerminalEventAndCleansToken() {
        Fixture fixture = new Fixture(context -> new RunHandle(context.input().toString(), RunStatus.SUCCEEDED, Optional.empty()));

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.queue.claimedBy).isEqualTo("worker-1");
        assertThat(fixture.queue.running).isTrue();
        assertThat(fixture.runtime.startedContext.get()).isNotNull();
        assertThat(fixture.eventStore.hasTerminalEvent("run-1")).isTrue();
        assertThat(fixture.projections.terminalStatus).isEqualTo("COMPLETED");
        assertThat(fixture.queue.terminalStatus).isEqualTo("COMPLETED");
        assertThat(fixture.audit.actions).contains("run.worker.started", "run.worker.completed");
        assertThat(fixture.registry.activeToken("run-1")).isEmpty();
    }

    @Test
    void cancelledRunMarksCancelledAndDoesNotDuplicateTerminalEvent() {
        Fixture fixture = new Fixture(context -> {
            context.cancellationToken().cancel("user");
            return new RunHandle("run-1", RunStatus.CANCELLED, Optional.empty());
        });

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.eventStore.hasTerminalEvent("run-1")).isTrue();
        assertThat(fixture.publisher.cancelledPublishes).isEqualTo(1);
        assertThat(fixture.projections.terminalStatus).isEqualTo("CANCELLED");
        assertThat(fixture.projections.failure).containsEntry("reason", "user");
        assertThat(fixture.queue.terminalStatus).isEqualTo("CANCELLED");
        assertThat(fixture.audit.actions).contains("run.worker.cancelled");
        assertThat(fixture.registry.activeToken("run-1")).isEmpty();
    }

    @Test
    void timeoutCancelsRuntimeMarksTimedOutPublishesFailedTerminalEventAndAudits() {
        Fixture fixture = new Fixture(context -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new RunHandle("run-1", RunStatus.RUNNING, Optional.empty());
        }, Duration.ofMillis(25));

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.runtime.cancelledRunId.get()).isEqualTo("run-1");
        assertThat(fixture.runtime.cancelReason.get()).isEqualTo("timeout");
        assertThat(fixture.eventStore.hasTerminalEvent("run-1")).isTrue();
        assertThat(fixture.publisher.timedOutPublishes).isEqualTo(1);
        assertThat(fixture.projections.terminalStatus).isEqualTo("TIMED_OUT");
        assertThat(fixture.queue.terminalStatus).isEqualTo("TIMED_OUT");
        assertThat(fixture.audit.actions).contains("run.worker.timed_out");
        assertThat(fixture.registry.activeToken("run-1")).isEmpty();
    }

    @Test
    void failureMarksFailedPublishesTerminalEventAndRemovesCancellationToken() {
        Fixture fixture = new Fixture(context -> {
            throw new IllegalStateException("boom");
        });

        fixture.dispatcher.dispatch("worker-1");

        assertThat(fixture.eventStore.hasTerminalEvent("run-1")).isTrue();
        assertThat(fixture.publisher.failedPublishes).isEqualTo(1);
        assertThat(fixture.projections.terminalStatus).isEqualTo("FAILED");
        assertThat(fixture.projections.failure).containsEntry("errorType", "IllegalStateException").containsEntry("message", "boom");
        assertThat(fixture.queue.terminalStatus).isEqualTo("FAILED");
        assertThat(fixture.audit.actions).contains("run.worker.failed");
        assertThat(fixture.registry.activeToken("run-1")).isEmpty();
    }

    private static final class Fixture {
        private final FakeRunQueue queue = new FakeRunQueue();
        private final FakeProjectionRepository projections = new FakeProjectionRepository();
        private final FakeRunEventStore eventStore = new FakeRunEventStore();
        private final FakeTerminalPublisher publisher = new FakeTerminalPublisher(eventStore);
        private final InMemoryCancellationRegistry registry = new InMemoryCancellationRegistry();
        private final FakeAuditRepository audit = new FakeAuditRepository();
        private final FakeRuntime runtime;
        private final DefaultRunDispatcher dispatcher;

        Fixture(RuntimeStart start) {
            this(start, Duration.ofSeconds(5));
        }

        Fixture(RuntimeStart start, Duration timeout) {
            runtime = new FakeRuntime(start);
            dispatcher = new DefaultRunDispatcher(queue, projections, eventStore, publisher, registry, audit, runtime, CLOCK, timeout);
        }
    }

    @FunctionalInterface
    private interface RuntimeStart {
        RunHandle start(RunContext context);
    }

    private static final class FakeRuntime implements AgentRuntime {
        private final RuntimeStart start;
        private final AtomicReference<RunContext> startedContext = new AtomicReference<>();
        private final AtomicReference<String> cancelledRunId = new AtomicReference<>();
        private final AtomicReference<String> cancelReason = new AtomicReference<>();

        private FakeRuntime(RuntimeStart start) {
            this.start = start;
        }

        @Override
        public RunHandle start(RunContext context) {
            startedContext.set(context);
            return start.start(context);
        }

        @Override
        public void cancel(String runId, String reason) {
            cancelledRunId.set(runId);
            cancelReason.set(reason);
        }
    }

    private static final class FakeRunQueue implements RunQueue {
        private final QueuedRun run = new QueuedRun("run-1", "session-1", "tenant-1", "user-1", "workspace-1", "trace-1", "correlation-1", "chat", Map.of("text", "hello"), CLOCK.instant(), 0);
        private boolean claimed;
        private boolean running;
        private String claimedBy;
        private String terminalStatus;

        @Override public void enqueue(QueuedRun run) { }

        @Override
        public Optional<QueuedRun> claimNext(String workerId, Instant now) {
            if (claimed) {
                return Optional.empty();
            }
            claimed = true;
            claimedBy = workerId;
            return Optional.of(run);
        }

        @Override public boolean markRunning(String runId, Instant startedAt) { running = true; return true; }
        @Override public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { this.terminalStatus = terminalStatus; return true; }
        @Override public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return Optional.empty(); }
        @Override public boolean removeIfTerminal(String runId) { return false; }
    }

    private static final class FakeProjectionRepository implements RunProjectionRepository {
        private String terminalStatus;
        private Map<String, Object> failure = Map.of();

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
        private final AtomicBoolean terminal = new AtomicBoolean();
        @Override public void append(RunEvent event) { terminal.set(true); }
        @Override public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return List.of(); }
        @Override public Optional<RunEvent> findLastByRun(String runId) { return Optional.empty(); }
        @Override public boolean hasTerminalEvent(String runId) { return terminal.get(); }
    }

    private static final class FakeTerminalPublisher implements RunTerminalEventPublisher {
        private final FakeRunEventStore store;
        private int cancelledPublishes;
        private int timedOutPublishes;
        private int failedPublishes;

        private FakeTerminalPublisher(FakeRunEventStore store) { this.store = store; }
        @Override public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) { store.terminal.set(true); return true; }
        @Override public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) { cancelledPublishes++; store.terminal.set(true); return true; }
        @Override public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) { failedPublishes++; store.terminal.set(true); return true; }
        @Override public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) { timedOutPublishes++; store.terminal.set(true); return true; }
    }

    private static final class FakeAuditRepository implements AuditRepository {
        private final List<String> actions = new ArrayList<>();
        @Override public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) { actions.add(action); }
    }
}
