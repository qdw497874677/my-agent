package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.domain.common.PlatformIds.*;
import io.github.pi_java.agent.domain.event.*;
import io.github.pi_java.agent.domain.runtime.RunStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public final class DefaultRunTerminalEventPublisher implements RunTerminalEventPublisher {

    private static final String NO_STEP_ID = "step-none";
    private static final String DEFAULT_CAUSATION_ID = "terminal-event";

    private final RunEventStore runEventStore;
    private final EventSink eventSink;
    private final Clock clock;
    private final Supplier<String> eventIdSupplier;

    public DefaultRunTerminalEventPublisher(
            RunEventStore runEventStore,
            EventSink eventSink,
            Clock clock,
            Supplier<String> eventIdSupplier) {
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventIdSupplier = Objects.requireNonNull(eventIdSupplier, "eventIdSupplier must not be null");
    }

    @Override
    public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) {
        return publishIfAbsent(run, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED,
                Map.of("status", "SUCCEEDED"), timestamp(finishedAt));
    }

    @Override
    public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) {
        return publishIfAbsent(run, RunEventType.RUN_CANCELLED, RunStatus.CANCELLED,
                Map.of("status", "CANCELLED", "reason", normalize(reason, "cancelled")), timestamp(finishedAt));
    }

    @Override
    public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) {
        return publishIfAbsent(run, RunEventType.RUN_FAILED, RunStatus.FAILED,
                Map.of("status", "FAILED", "errorType", normalize(errorType, "ERROR"), "message", normalize(message, "failed")),
                timestamp(finishedAt));
    }

    @Override
    public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) {
        return publishIfAbsent(run, RunEventType.RUN_FAILED, RunStatus.FAILED,
                Map.of("status", "TIMED_OUT", "reason", normalize(reason, "timed out")), timestamp(finishedAt));
    }

    private boolean publishIfAbsent(QueuedRun run, RunEventType type, RunStatus status, Map<String, Object> attributes, Instant timestamp) {
        if (runEventStore.hasTerminalEvent(run.runId())) {
            return false;
        }
        long sequence = runEventStore.findLastByRun(run.runId())
                .map(last -> last.sequence() + 1)
                .orElse(1L);
        RunEvent event = new RunEvent(
                eventIdSupplier.get(),
                new TenantId(run.tenantId()),
                new UserId(run.userId()),
                new SessionId(run.sessionId()),
                new RunId(run.runId()),
                new StepId(NO_STEP_ID),
                new WorkspaceId(run.workspaceId()),
                sequence,
                timestamp,
                type,
                new TraceId(run.traceId()),
                new CorrelationId(run.correlationId()),
                new CausationId(DEFAULT_CAUSATION_ID),
                new RunEventPayload.ExtensionPayload(type.wireName(), "1", attributes),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
        eventSink.publish(event);
        return true;
    }

    private Instant timestamp(Instant supplied) {
        return supplied == null ? clock.instant() : supplied;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
