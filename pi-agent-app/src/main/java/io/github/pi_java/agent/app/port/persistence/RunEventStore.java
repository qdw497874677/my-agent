package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.domain.event.RunEvent;

import java.util.List;
import java.util.Optional;

public interface RunEventStore {

    void append(RunEvent event);

    List<RunEvent> listByRun(String runId, long afterSequence, int limit);

    Optional<RunEvent> findLastByRun(String runId);

    boolean hasTerminalEvent(String runId);
}
