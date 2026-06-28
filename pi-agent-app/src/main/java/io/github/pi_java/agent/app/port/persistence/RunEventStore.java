package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.domain.event.RunEvent;

import java.util.List;
import java.util.Optional;

public interface RunEventStore {

    void append(RunEvent event);

    List<RunEvent> listByRun(String runId, long afterSequence, int limit);

    Optional<RunEvent> findLastByRun(String runId);

    boolean hasTerminalEvent(String runId);

    /**
     * Lists run events scoped to {@code sessionId}/{@code runId} for the caller
     * defined by {@link RequestContext}.
     *
     * <p>This is the ownership-safe event read path used by the App transcript
     * assembler (decision D-15): unlike {@link #listByRun(String, long, int)},
     * it carries the request context plus both session and run identifiers so
     * infrastructure implementations can enforce tenant/user/session/run
     * filters at the query/SQL layer before returning audit/replay events
     * (decisions D-05, D-14, D-15).
     *
     * <p>This method is a default that throws until Phase 16 plan 03 provides
     * the ownership-aware JDBC implementation; it is declared as a default so
     * existing test fakes and infrastructure stubs continue to compile.
     *
     * @return events for the run in ascending sequence order, never {@code null}.
     */
    default List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
        throw new UnsupportedOperationException(
                "listBySessionRun not yet implemented; Phase 16 plan 03 provides the ownership-aware implementation");
    }
}
