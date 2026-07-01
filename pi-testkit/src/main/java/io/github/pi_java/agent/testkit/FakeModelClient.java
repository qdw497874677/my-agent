package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.model.ModelClient;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FakeModelClient implements ModelClient {
    private final Queue<ModelResponse> scriptedResponses = new ArrayDeque<>();
    private final List<ModelRequest> requests = new CopyOnWriteArrayList<>();
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
        requests.add(Objects.requireNonNull(request, "request must not be null"));
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

    public Optional<ModelRequest> lastRequest() {
        if (requests.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(requests.get(requests.size() - 1));
    }

    public List<ModelRequest> requests() {
        return List.copyOf(requests);
    }
}
