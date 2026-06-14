package io.github.pi_java.agent.app.port.execution;

import java.time.Instant;
import java.util.Optional;

public interface RunQueue {

    void enqueue(QueuedRun run);

    Optional<QueuedRun> claimNext(String workerId, Instant now);

    boolean markRunning(String runId, Instant startedAt);

    boolean markTerminal(String runId, String terminalStatus, Instant finishedAt);

    /**
     * Atomically transitions only a queued or claimed run to cancellation and returns the original queued payload.
     * The returned payload is required by App services to publish an idempotent durable terminal run.cancelled event.
     */
    Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt);

    boolean removeIfTerminal(String runId);
}
