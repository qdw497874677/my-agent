package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.*;
import io.github.pi_java.agent.domain.common.PlatformIds.*;
import io.github.pi_java.agent.domain.event.*;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRunCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-14T05:30:00Z");

    @Test
    void createRunPersistsAuditsAndEnqueues() {
        Fixtures fx = new Fixtures();
        DefaultRunCommandService service = fx.service();
        CreateRunRequest request = new CreateRunRequest("agent-1", "task", Map.of("prompt", "hello"), "workspace-1", Map.of());

        RunResponse response = service.createRun(context(), "session-1", request);

        assertThat(response.runId()).isEqualTo("run-1");
        assertThat(fx.projections.createdRunId).isEqualTo("run-1");
        assertThat(fx.queue.enqueued.runId()).isEqualTo("run-1");
        assertThat(fx.audit.actions).containsExactly("run.create");
    }

    @Test
    void cancelQueuedRunUpdatesDurableStateAuditQueueAndPublishesCancelledEvent() {
        Fixtures fx = new Fixtures();
        fx.projections.status = status("QUEUED", false);
        fx.queue.cancelledQueuedRun = Optional.of(queuedRun());

        RunStatusResponse response = fx.service().cancelRun(context(), "session-1", "run-1", new CancelRunRequest("user requested"));

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(fx.projections.requestedCancellationReason).isEqualTo("user requested");
        assertThat(fx.projections.markedTerminalStatus).isEqualTo("CANCELLED");
        assertThat(fx.queue.markedTerminalStatus).isEqualTo("CANCELLED");
        assertThat(fx.terminalPublisher.cancelledReasons).containsExactly("user requested");
        assertThat(fx.audit.actions).containsExactly("run.cancel.requested");
    }

    @Test
    void cancelRunningRunSignalsCancellationRegistry() {
        Fixtures fx = new Fixtures();
        fx.projections.status = status("RUNNING", false);
        fx.queue.cancelledQueuedRun = Optional.empty();

        fx.service().cancelRun(context(), "session-1", "run-1", new CancelRunRequest("stop"));

        assertThat(fx.registry.cancelledRunId).isEqualTo("run-1");
        assertThat(fx.registry.reason).isEqualTo("stop");
        assertThat(fx.terminalPublisher.cancelledReasons).isEmpty();
    }

    @Test
    void cancelTerminalRunIsIdempotentAndDoesNotDuplicateTerminalState() {
        Fixtures fx = new Fixtures();
        fx.projections.status = status("SUCCEEDED", true);

        RunStatusResponse response = fx.service().cancelRun(context(), "session-1", "run-1", new CancelRunRequest("too late"));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(fx.audit.actions).containsExactly("run.cancel.noop_terminal");
        assertThat(fx.projections.requestedCancellationReason).isNull();
        assertThat(fx.projections.markedTerminalStatus).isNull();
        assertThat(fx.terminalPublisher.cancelledReasons).isEmpty();
    }

    @Test
    void terminalEventPublisherSkipsWhenTerminalEventAlreadyExists() {
        RecordingRunEventStore store = new RecordingRunEventStore();
        store.hasTerminal = true;
        RecordingEventSink sink = new RecordingEventSink();
        DefaultRunTerminalEventPublisher publisher = new DefaultRunTerminalEventPublisher(
                store, sink, Clock.fixed(NOW, ZoneOffset.UTC), () -> "event-terminal");

        assertThat(publisher.publishCancelledIfAbsent(queuedRun(), "stop", NOW)).isFalse();
        assertThat(sink.published).isEmpty();
    }

    static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    static RunStatusResponse status(String status, boolean terminal) {
        return new RunStatusResponse("session-1", "run-1", status, terminal, NOW, "trace-1", "correlation-1");
    }

    static QueuedRun queuedRun() {
        return new QueuedRun("run-1", "session-1", "tenant-1", "user-1", "workspace-1",
                "trace-1", "correlation-1", "task", Map.of("prompt", "hello"), NOW, 0);
    }

    private static final class Fixtures {
        final RecordingRunProjectionRepository projections = new RecordingRunProjectionRepository();
        final RecordingRunQueue queue = new RecordingRunQueue();
        final RecordingCancellationRegistry registry = new RecordingCancellationRegistry();
        final RecordingRunTerminalEventPublisher terminalPublisher = new RecordingRunTerminalEventPublisher();
        final RecordingAuditRepository audit = new RecordingAuditRepository();

        DefaultRunCommandService service() {
            return new DefaultRunCommandService(projections, queue, registry, terminalPublisher, audit,
                    () -> "run-1", Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }

    private static final class RecordingRunProjectionRepository implements RunProjectionRepository {
        RunStatusResponse status = status("QUEUED", false);
        String createdRunId;
        String requestedCancellationReason;
        String markedTerminalStatus;

        @Override public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { createdRunId = runId; }
        @Override public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return Optional.of(new RunResponse(context.tenantId(), context.userId(), sessionId, runId, "workspace-1", status.status(), context.traceId(), context.correlationId(), NOW, NOW)); }
        @Override public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return status; }
        @Override public boolean markRunning(String runId, Instant startedAt) { return false; }
        @Override public boolean requestCancellation(String runId, String reason, Instant requestedAt) { requestedCancellationReason = reason; return true; }
        @Override public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { markedTerminalStatus = status; this.status = status(status, true); return true; }
        @Override public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return null; }
        @Override public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return null; }
    }

    private static final class RecordingRunQueue implements RunQueue {
        QueuedRun enqueued;
        Optional<QueuedRun> cancelledQueuedRun = Optional.empty();
        String markedTerminalStatus;
        @Override public void enqueue(QueuedRun run) { enqueued = run; }
        @Override public Optional<QueuedRun> claimNext(String workerId, Instant now) { return Optional.empty(); }
        @Override public boolean markRunning(String runId, Instant startedAt) { return false; }
        @Override public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { markedTerminalStatus = terminalStatus; return true; }
        @Override public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return cancelledQueuedRun; }
        @Override public boolean removeIfTerminal(String runId) { return false; }
    }

    private static final class RecordingCancellationRegistry implements CancellationRegistry {
        String cancelledRunId;
        String reason;
        @Override public CancellationToken tokenFor(String runId) { return new CancellationToken(); }
        @Override public Optional<CancellationToken> activeToken(String runId) { return Optional.empty(); }
        @Override public boolean requestCancellation(String runId, String reason) { this.cancelledRunId = runId; this.reason = reason; return true; }
        @Override public void remove(String runId) { }
    }

    private static final class RecordingRunTerminalEventPublisher implements RunTerminalEventPublisher {
        final List<String> cancelledReasons = new ArrayList<>();
        @Override public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) { return false; }
        @Override public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) { cancelledReasons.add(reason); return true; }
        @Override public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) { return false; }
        @Override public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) { return false; }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        final List<String> actions = new ArrayList<>();
        @Override public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) { actions.add(action); }
    }

    private static final class RecordingRunEventStore implements RunEventStore {
        boolean hasTerminal;
        @Override public void append(RunEvent event) { }
        @Override public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return List.of(); }
        @Override public Optional<RunEvent> findLastByRun(String runId) { return Optional.empty(); }
        @Override public boolean hasTerminalEvent(String runId) { return hasTerminal; }
    }

    private static final class RecordingEventSink implements EventSink {
        final List<RunEvent> published = new ArrayList<>();
        @Override public void publish(RunEvent event) { published.add(event); }
    }
}
