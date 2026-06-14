package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class OpenAiCompatibleStreamingModelClient implements StreamingModelClient {
    private final OpenAiProviderProperties properties;
    private final SecretResolver secretResolver;
    private final OpenAiSpringAiModelFactory modelFactory;
    private final ProviderResiliencePolicy resiliencePolicy;

    public OpenAiCompatibleStreamingModelClient(OpenAiProviderProperties properties, SecretResolver secretResolver,
                                                OpenAiSpringAiModelFactory modelFactory) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.secretResolver = Objects.requireNonNull(secretResolver, "secretResolver must not be null");
        this.modelFactory = Objects.requireNonNull(modelFactory, "modelFactory must not be null");
        this.resiliencePolicy = new ProviderResiliencePolicy(properties.providerId(), properties.resilience());
    }

    @Override
    public void stream(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");
        Objects.requireNonNull(sink, "sink must not be null");
        Instant started = Instant.now();
        AtomicLong sequence = new AtomicLong();
        String modelRef = request.context().agentDefinition().modelRef();
        RunId runId = new RunId(request.context().workspaceScope().runId());
        OpenAiToolCallAccumulator accumulator = new OpenAiToolCallAccumulator(runId, new StepId("model-step"));
        ResolvedSecret secret = secretResolver.resolve(properties.credentialRef());
        OpenAiSpringAiModelFactory.ModelConfig config = new OpenAiSpringAiModelFactory.ModelConfig(properties.providerId(),
                properties.defaultModelId(), properties.baseUrl(), properties.completionsPath(), secret.rawValue(),
                stringMapToObjectMap(properties.defaultParameters()), stringMapToObjectMap(properties.extraBody()));

        Iterable<OpenAiStreamEvent> events;
        try {
            events = resiliencePolicy.executeBeforeStream(() -> modelFactory.create(config).stream(promptFrom(request), cancellationToken));
        } catch (RuntimeException ex) {
            sink.accept(error(sequence, started, modelRef, OpenAiProviderErrorMapper.fromThrowable(ex, secret.rawValue(), true)));
            return;
        }

        boolean emitted = false;
        for (OpenAiStreamEvent event : events) {
            if (event instanceof OpenAiStreamEvent.OnSubscribe onSubscribe) {
                onSubscribe.action().run();
                continue;
            }
            if (cancellationToken.isCancellationRequested()) {
                sink.accept(new ModelStreamChunk.Cancelled(properties.providerId(), properties.defaultModelId(), modelRef,
                        sequence.getAndIncrement(), latency(started), cancellationToken.reason().orElse("cancelled")));
                return;
            }
            if (event instanceof OpenAiStreamEvent.Text text && text.delta() != null && !text.delta().isEmpty()) {
                emitted = true;
                sink.accept(new ModelStreamChunk.TextDelta(properties.providerId(), properties.defaultModelId(), modelRef,
                        sequence.getAndIncrement(), latency(started), text.delta()));
            } else if (event instanceof OpenAiStreamEvent.ToolCallFragment tool) {
                accumulator.add(new OpenAiToolCallAccumulator.Fragment(tool.index(), tool.id(), tool.name(), tool.argumentsDelta()))
                        .ifPresent(toolCall -> {
                            sink.accept(new ModelStreamChunk.ToolCallIntent(properties.providerId(), properties.defaultModelId(), modelRef,
                                    sequence.getAndIncrement(), latency(started), toolCall));
                        });
                emitted = true;
            } else if (event instanceof OpenAiStreamEvent.Usage usage && usage.usage() != null) {
                sink.accept(new ModelStreamChunk.Usage(properties.providerId(), properties.defaultModelId(), modelRef,
                        sequence.getAndIncrement(), latency(started), usage.usage()));
            } else if (event instanceof OpenAiStreamEvent.Finish finish) {
                accumulator.validateComplete().ifPresent(error -> sink.accept(error(sequence, started, modelRef, error)));
                sink.accept(new ModelStreamChunk.Finished(properties.providerId(), properties.defaultModelId(), modelRef,
                        sequence.getAndIncrement(), latency(started), toFinishReason(finish.reason()), finish.usage()));
            } else if (event instanceof OpenAiStreamEvent.Error errorEvent) {
                sink.accept(error(sequence, started, modelRef,
                        OpenAiProviderErrorMapper.fromThrowable(errorEvent.throwable(), secret.rawValue(), !emitted)));
                return;
            }
        }
    }

    private ModelStreamChunk.ProviderError error(AtomicLong sequence, Instant started, String modelRef,
                                                 io.github.pi_java.agent.domain.model.ProviderErrorSummary summary) {
        return new ModelStreamChunk.ProviderError(properties.providerId(), properties.defaultModelId(), modelRef,
                sequence.getAndIncrement(), latency(started), summary);
    }

    private static String promptFrom(ModelRequest request) {
        if (request.context().input() instanceof io.github.pi_java.agent.domain.runtime.RunInput.ChatInput chat) {
            return chat.text();
        }
        if (request.context().input() instanceof io.github.pi_java.agent.domain.runtime.RunInput.TaskInput task) {
            return task.objective();
        }
        return request.context().input().toString();
    }

    private static ModelFinishReason toFinishReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return ModelFinishReason.UNKNOWN;
        }
        return switch (reason.toLowerCase()) {
            case "stop" -> ModelFinishReason.STOP;
            case "length", "max_tokens" -> ModelFinishReason.LENGTH;
            case "tool_calls", "function_call" -> ModelFinishReason.TOOL_CALLS;
            case "content_filter", "safety" -> ModelFinishReason.CONTENT_FILTER;
            default -> ModelFinishReason.UNKNOWN;
        };
    }

    private static Duration latency(Instant started) {
        return Duration.between(started, Instant.now());
    }

    private static Map<String, Object> stringMapToObjectMap(Map<String, String> source) {
        return source.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
