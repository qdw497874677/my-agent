package io.github.pi_java.agent.app.port.execution;

import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.Optional;

public interface CancellationRegistry {

    CancellationToken tokenFor(String runId);

    Optional<CancellationToken> activeToken(String runId);

    boolean requestCancellation(String runId, String reason);

    void remove(String runId);
}
