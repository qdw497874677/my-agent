package io.github.pi_java.agent.app.port.execution;

import java.time.Instant;

public interface RunTerminalEventPublisher {

    /** Implementations must guard with RunEventStore.hasTerminalEvent(run.runId()) or an equivalent durable check. */
    boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt);

    /** Implementations must guard with RunEventStore.hasTerminalEvent(run.runId()) or an equivalent durable check. */
    boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt);

    /** Implementations must guard with RunEventStore.hasTerminalEvent(run.runId()) or an equivalent durable check. */
    boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt);

    /** Implementations must guard with RunEventStore.hasTerminalEvent(run.runId()) or an equivalent durable check. */
    boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt);
}
