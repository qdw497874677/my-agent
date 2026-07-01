package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolCall;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class FakeStreamingModelClient implements StreamingModelClient {
    private static final String DEFAULT_PROVIDER_ID = "fake-provider";
    private static final String DEFAULT_MODEL_ID = "fake-model";

    private final Queue<ScriptedAction> actions = new ArrayDeque<>();
    private final List<ModelRequest> requests = new CopyOnWriteArrayList<>();
    private String providerId = DEFAULT_PROVIDER_ID;
    private String modelId = DEFAULT_MODEL_ID;
    private long sequence;

    public FakeStreamingModelClient providerId(String providerId) {
        this.providerId = requireNonBlank(providerId, "providerId");
        return this;
    }

    public FakeStreamingModelClient modelId(String modelId) {
        this.modelId = requireNonBlank(modelId, "modelId");
        return this;
    }

    public FakeStreamingModelClient script(ModelStreamChunk chunk) {
        actions.add(new Emit(Objects.requireNonNull(chunk, "chunk must not be null")));
        return this;
    }

    public FakeStreamingModelClient text(String textDelta) {
        actions.add((request, cancellationToken, sink, client) -> sink.accept(
                new ModelStreamChunk.TextDelta(client.providerId, client.modelId, modelRef(request), client.nextSequence(),
                        Duration.ZERO, Objects.requireNonNull(textDelta, "textDelta must not be null"))));
        return this;
    }

    public FakeStreamingModelClient toolCall(ToolCall toolCall) {
        actions.add((request, cancellationToken, sink, client) -> sink.accept(
                new ModelStreamChunk.ToolCallIntent(client.providerId, client.modelId, modelRef(request), client.nextSequence(),
                        Duration.ZERO, Objects.requireNonNull(toolCall, "toolCall must not be null"))));
        return this;
    }

    public FakeStreamingModelClient usage(ModelUsage usage) {
        actions.add((request, cancellationToken, sink, client) -> sink.accept(
                new ModelStreamChunk.Usage(client.providerId, client.modelId, modelRef(request), client.nextSequence(),
                        Duration.ZERO, Objects.requireNonNull(usage, "usage must not be null"))));
        return this;
    }

    public FakeStreamingModelClient finish(ModelFinishReason finishReason) {
        return finish(finishReason, null);
    }

    public FakeStreamingModelClient finish(ModelFinishReason finishReason, ModelUsage usage) {
        actions.add((request, cancellationToken, sink, client) -> sink.accept(
                new ModelStreamChunk.Finished(client.providerId, client.modelId, modelRef(request), client.nextSequence(),
                        Duration.ZERO, Objects.requireNonNull(finishReason, "finishReason must not be null"), usage)));
        return this;
    }

    public FakeStreamingModelClient providerError(ProviderErrorSummary errorSummary) {
        actions.add((request, cancellationToken, sink, client) -> sink.accept(
                new ModelStreamChunk.ProviderError(client.providerId, client.modelId, modelRef(request), client.nextSequence(),
                        Duration.ZERO, Objects.requireNonNull(errorSummary, "errorSummary must not be null"))));
        return this;
    }

    public FakeStreamingModelClient delayed(Duration latency, ModelStreamChunk chunk) {
        Objects.requireNonNull(latency, "latency must not be null");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
        actions.add(new Emit(withLatency(Objects.requireNonNull(chunk, "chunk must not be null"), latency)));
        return this;
    }

    public FakeStreamingModelClient thenCancel(CancellationToken token, String reason) {
        actions.add((request, cancellationToken, sink, client) -> Objects.requireNonNull(token, "token must not be null")
                .cancel(Objects.requireNonNull(reason, "reason must not be null")));
        return this;
    }

    public FakeStreamingModelClient thenRun(Consumer<CancellationToken> action) {
        actions.add((request, cancellationToken, sink, client) -> Objects.requireNonNull(action, "action must not be null")
                .accept(cancellationToken));
        return this;
    }

    public FakeStreamingModelClient nextStream() {
        actions.add((request, cancellationToken, sink, client) -> client.stopCurrentStream = true);
        return this;
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

    @Override
    public void stream(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");
        Objects.requireNonNull(sink, "sink must not be null");
        requests.add(request);
        stopCurrentStream = false;
        while (!actions.isEmpty() && !stopCurrentStream) {
            if (cancellationToken.isCancellationRequested()) {
                sink.accept(new ModelStreamChunk.Cancelled(providerId, modelId, modelRef(request), nextSequence(), Duration.ZERO,
                        cancellationToken.reason().orElse("cancelled")));
                return;
            }
            actions.poll().run(request, cancellationToken, sink, this);
        }
    }

    private long nextSequence() {
        return ++sequence;
    }

    private boolean stopCurrentStream;

    private static String modelRef(ModelRequest request) {
        return request.context().agentDefinition().modelRef();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static ModelStreamChunk withLatency(ModelStreamChunk chunk, Duration latency) {
        if (chunk instanceof ModelStreamChunk.TextDelta textDelta) {
            return new ModelStreamChunk.TextDelta(textDelta.providerId(), textDelta.modelId(), textDelta.modelRef(),
                    textDelta.sequence(), latency, textDelta.textDelta());
        }
        if (chunk instanceof ModelStreamChunk.ToolCallIntent toolCallIntent) {
            return new ModelStreamChunk.ToolCallIntent(toolCallIntent.providerId(), toolCallIntent.modelId(),
                    toolCallIntent.modelRef(), toolCallIntent.sequence(), latency, toolCallIntent.toolCall());
        }
        if (chunk instanceof ModelStreamChunk.Usage usage) {
            return new ModelStreamChunk.Usage(usage.providerId(), usage.modelId(), usage.modelRef(), usage.sequence(),
                    latency, usage.usage());
        }
        if (chunk instanceof ModelStreamChunk.Finished finished) {
            return new ModelStreamChunk.Finished(finished.providerId(), finished.modelId(), finished.modelRef(),
                    finished.sequence(), latency, finished.finishReason(), finished.usage());
        }
        if (chunk instanceof ModelStreamChunk.Cancelled cancelled) {
            return new ModelStreamChunk.Cancelled(cancelled.providerId(), cancelled.modelId(), cancelled.modelRef(),
                    cancelled.sequence(), latency, cancelled.reason());
        }
        if (chunk instanceof ModelStreamChunk.TimedOut timedOut) {
            return new ModelStreamChunk.TimedOut(timedOut.providerId(), timedOut.modelId(), timedOut.modelRef(),
                    timedOut.sequence(), latency, timedOut.timeout());
        }
        ModelStreamChunk.ProviderError providerError = (ModelStreamChunk.ProviderError) chunk;
        return new ModelStreamChunk.ProviderError(providerError.providerId(), providerError.modelId(), providerError.modelRef(),
                providerError.sequence(), latency, providerError.errorSummary());
    }

    @FunctionalInterface
    private interface ScriptedAction {
        void run(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink,
                 FakeStreamingModelClient client);
    }

    private record Emit(ModelStreamChunk chunk) implements ScriptedAction {
        @Override
        public void run(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink,
                        FakeStreamingModelClient client) {
            sink.accept(chunk);
        }
    }
}
