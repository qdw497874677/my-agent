package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.runtime.CancellationToken;

public interface ModelClient {
    ModelResponse next(ModelRequest request, CancellationToken cancellationToken);
}
