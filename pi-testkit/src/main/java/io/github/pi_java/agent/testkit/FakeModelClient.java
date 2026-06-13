package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.model.ModelClient;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.ArrayDeque;
import java.util.Queue;

public final class FakeModelClient implements ModelClient {
    private final Queue<ModelResponse> scriptedResponses = new ArrayDeque<>();
    private CancellationToken tokenToCancel;
    private String cancellationReason;

    public FakeModelClient script(ModelResponse response) {
        scriptedResponses.add(response);
        return this;
    }

    public FakeModelClient scriptThenCancel(ModelResponse response, CancellationToken token, String reason) {
        scriptedResponses.add(response);
        tokenToCancel = token;
        cancellationReason = reason;
        return this;
    }

    @Override
    public ModelResponse next(ModelRequest request, CancellationToken cancellationToken) {
        ModelResponse response = scriptedResponses.poll();
        if (response == null) {
            throw new IllegalStateException("no scripted model response available");
        }
        if (tokenToCancel != null) {
            tokenToCancel.cancel(cancellationReason);
            tokenToCancel = null;
        }
        return response;
    }
}
