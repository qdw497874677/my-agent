package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@FunctionalInterface
public interface OpenAiSpringAiModelFactory {
    OpenAiStreamSource create(ModelConfig config);

    static OpenAiSpringAiModelFactory springAi() {
        return config -> new SpringAiStreamSource(config, buildModel(config));
    }

    private static OpenAiChatModel buildModel(ModelConfig config) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .completionsPath(config.completionsPath())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.modelId())
                .streamUsage(true)
                .extraBody(config.extraBody())
                .internalToolExecutionEnabled(false)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    record ModelConfig(String providerId, String modelId, String baseUrl, String completionsPath, String apiKey,
                       Map<String, Object> defaultParameters, Map<String, Object> extraBody) {
        public ModelConfig {
            providerId = requireNonBlank(providerId, "providerId");
            modelId = requireNonBlank(modelId, "modelId");
            baseUrl = requireNonBlank(baseUrl, "baseUrl");
            completionsPath = requireNonBlank(completionsPath, "completionsPath");
            apiKey = requireNonBlank(apiKey, "apiKey");
            defaultParameters = Map.copyOf(Objects.requireNonNull(defaultParameters, "defaultParameters must not be null"));
            extraBody = Map.copyOf(Objects.requireNonNull(extraBody, "extraBody must not be null"));
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    final class SpringAiStreamSource implements OpenAiStreamSource {
        private final ModelConfig config;
        private final OpenAiChatModel model;

        private SpringAiStreamSource(ModelConfig config, OpenAiChatModel model) {
            this.config = config;
            this.model = model;
        }

        @Override
        public Iterable<OpenAiStreamEvent> stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken) {
            Flux<OpenAiStreamEvent> events = model.stream(new Prompt(OpenAiSpringAiModelFactory.toSpringAiMessages(messages)))
                    .concatMapIterable(this::toEvents);
            return () -> new QueueBackedOpenAiStreamIterator(events, cancellationToken);
        }

        private List<OpenAiStreamEvent> toEvents(ChatResponse response) {
            List<OpenAiStreamEvent> events = new ArrayList<>();
            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            if (usage != null) {
                events.add(OpenAiStreamEvent.usage(new ModelUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens())));
            }
            for (Generation generation : response.getResults()) {
                AssistantMessage message = generation.getOutput();
                if (message.getText() != null && !message.getText().isEmpty()) {
                    events.add(OpenAiStreamEvent.text(message.getText()));
                }
                int index = 0;
                for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                    events.add(OpenAiStreamEvent.toolCall(index++, toolCall.id(), toolCall.name(), toolCall.arguments()));
                }
                String finishReason = generation.getMetadata() == null ? null : generation.getMetadata().getFinishReason();
                if (finishReason != null && !finishReason.isBlank()) {
                    events.add(OpenAiStreamEvent.finish(finishReason, null));
                }
            }
            return events;
        }
    }

    final class QueueBackedOpenAiStreamIterator implements Iterator<OpenAiStreamEvent>, AutoCloseable {
        private static final int MAX_BUFFERED_SIGNALS = 256;
        private static final long OFFER_TIMEOUT_MILLIS = 250;

        private final BlockingQueue<StreamSignal> queue = new LinkedBlockingQueue<>(MAX_BUFFERED_SIGNALS);
        private final CancellationToken cancellationToken;
        private final AtomicReference<Disposable> disposable = new AtomicReference<>();
        private OpenAiStreamEvent next;
        private boolean done;

        QueueBackedOpenAiStreamIterator(Flux<OpenAiStreamEvent> events, CancellationToken cancellationToken) {
            this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");
            Disposable subscription = events.subscribe(
                    event -> offer(new EventSignal(event)),
                    throwable -> offer(new ErrorSignal(throwable)),
                    () -> offer(CompleteSignal.INSTANCE));
            disposable.set(subscription);
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (done) {
                return false;
            }
            while (true) {
                if (cancellationToken.isCancellationRequested()) {
                    dispose();
                    done = true;
                    return false;
                }
                StreamSignal signal;
                try {
                    signal = queue.poll(25, TimeUnit.MILLISECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    dispose();
                    next = OpenAiStreamEvent.error(interrupted);
                    done = true;
                    return true;
                }
                if (signal == null) {
                    continue;
                }
                if (signal instanceof EventSignal eventSignal) {
                    next = eventSignal.event();
                    return true;
                }
                if (signal instanceof ErrorSignal errorSignal) {
                    next = OpenAiStreamEvent.error(errorSignal.throwable());
                    done = true;
                    return true;
                }
                done = true;
                dispose();
                return false;
            }
        }

        @Override
        public OpenAiStreamEvent next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            OpenAiStreamEvent current = next;
            next = null;
            return current;
        }

        private void offer(StreamSignal signal) {
            if (done) {
                return;
            }
            try {
                if (!queue.offer(signal, OFFER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    disposeSubscription();
                    queue.clear();
                    queue.offer(new ErrorSignal(new TimeoutException("OpenAI stream consumer did not keep up with provider events")));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                disposeSubscription();
                queue.clear();
                queue.offer(new ErrorSignal(interrupted));
            }
        }

        private void dispose() {
            close();
        }

        @Override
        public void close() {
            done = true;
            disposeSubscription();
        }

        private void disposeSubscription() {
            Disposable subscription = disposable.get();
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }

    sealed interface StreamSignal permits EventSignal, ErrorSignal, CompleteSignal {
    }

    record EventSignal(OpenAiStreamEvent event) implements StreamSignal {
    }

    record ErrorSignal(Throwable throwable) implements StreamSignal {
    }

    enum CompleteSignal implements StreamSignal {
        INSTANCE
    }

    static List<Message> toSpringAiMessages(List<OpenAiChatMessage> messages) {
        return messages.stream()
                .<Message>map(message -> switch (message.role()) {
                    case "user" -> new UserMessage(message.content());
                    case "assistant" -> new AssistantMessage(message.content());
                    default -> throw new IllegalArgumentException("Unsupported OpenAI chat role: " + message.role());
                })
                .toList();
    }
}
