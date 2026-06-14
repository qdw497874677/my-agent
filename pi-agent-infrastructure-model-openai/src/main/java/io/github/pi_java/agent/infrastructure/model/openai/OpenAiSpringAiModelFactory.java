package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        public Iterable<OpenAiStreamEvent> stream(String prompt, CancellationToken cancellationToken) {
            List<OpenAiStreamEvent> events = new CopyOnWriteArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Disposable disposable = model.stream(new Prompt(new UserMessage(prompt))).subscribe(response -> events.addAll(toEvents(response)),
                    throwable -> {
                        failure.set(throwable);
                        done.countDown();
                    }, done::countDown);
            try {
                while (!done.await(25, TimeUnit.MILLISECONDS)) {
                    if (cancellationToken.isCancellationRequested()) {
                        disposable.dispose();
                        break;
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                disposable.dispose();
                events.add(OpenAiStreamEvent.error(interrupted));
            }
            if (failure.get() != null) {
                events.add(OpenAiStreamEvent.error(failure.get()));
            }
            return events;
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
}
