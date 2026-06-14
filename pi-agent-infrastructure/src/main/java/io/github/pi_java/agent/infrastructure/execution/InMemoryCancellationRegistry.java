package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCancellationRegistry implements CancellationRegistry {

    private final ConcurrentHashMap<String, CancellationToken> tokens = new ConcurrentHashMap<>();

    @Override
    public CancellationToken tokenFor(String runId) {
        return tokens.computeIfAbsent(runId, ignored -> new CancellationToken());
    }

    @Override
    public Optional<CancellationToken> activeToken(String runId) {
        return Optional.ofNullable(tokens.get(runId));
    }

    @Override
    public boolean requestCancellation(String runId, String reason) {
        CancellationToken token = tokens.get(runId);
        if (token == null) {
            return false;
        }
        token.cancel(reason);
        return true;
    }

    @Override
    public void remove(String runId) {
        tokens.remove(runId);
    }
}
