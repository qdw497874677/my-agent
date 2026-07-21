package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleStreamingModelClientTest {

    private static final SecretRef SECRET_REF = SecretRef.of("env:OPENAI_API_KEY");
    private static final CredentialRef CREDENTIAL_REF = new CredentialRef(SECRET_REF);

    @Test
    void factoryResolvesSecretAtBoundaryAndBuildsSpringAiModelConfiguration() {
        CapturingSpringAiModelFactory factory = new CapturingSpringAiModelFactory(List.of(
                OpenAiStreamEvent.text("hello"),
                OpenAiStreamEvent.finish("stop", new ModelUsage(3, 4, 7))));

        OpenAiCompatibleStreamingModelClient client = new OpenAiCompatibleStreamingModelClient(
                providerProperties(), secretResolver("sk-secret-value"), factory);

        List<ModelStreamChunk> chunks = new ArrayList<>();
        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(factory.captured().apiKey()).isEqualTo("sk-secret-value");
        assertThat(factory.captured().baseUrl()).isEqualTo("https://example.test/v1");
        assertThat(factory.captured().completionsPath()).isEqualTo("/chat/completions");
        assertThat(factory.captured().modelId()).isEqualTo("gpt-test");
        assertThat(factory.captured().extraBody()).containsEntry("custom", "value");
        assertThat(chunks).extracting(ModelStreamChunk::modelRef).containsOnly("openai-compatible:gpt-test");
    }

    @Test
    void sendsPriorSessionMessagesThenCurrentInputAsOrderedProviderChatMessages() {
        CapturingSpringAiModelFactory factory = new CapturingSpringAiModelFactory(List.of(
                OpenAiStreamEvent.text("context-aware"),
                OpenAiStreamEvent.finish("stop", null)));
        OpenAiCompatibleStreamingModelClient client = new OpenAiCompatibleStreamingModelClient(
                providerProperties(), secretResolver("sk-secret-value"), factory);

        client.stream(requestWithHistory(new RunInput.ChatInput("current question")), new CancellationToken(), chunk -> { });

        assertThat(factory.capturedMessages()).extracting(OpenAiChatMessage::role)
                .containsExactly("user", "assistant", "user");
        assertThat(factory.capturedMessages()).extracting(OpenAiChatMessage::content)
                .containsExactly("prior question", "prior answer", "current question");
        assertThat(factory.capturedMessages()).filteredOn(message -> message.content().equals("current question"))
                .hasSize(1);
    }

    @Test
    void normalizesOnlySupportedHistoricalRolesAndAppendsTaskInputOnce() {
        CapturingSpringAiModelFactory factory = new CapturingSpringAiModelFactory(List.of(OpenAiStreamEvent.finish("stop", null)));
        OpenAiCompatibleStreamingModelClient client = new OpenAiCompatibleStreamingModelClient(
                providerProperties(), secretResolver("sk-secret-value"), factory);

        client.stream(requestWithHistory(new RunInput.TaskInput("complete the task")), new CancellationToken(), chunk -> { });

        assertThat(factory.capturedMessages()).extracting(OpenAiChatMessage::role)
                .containsExactly("user", "assistant", "user");
        assertThat(factory.capturedMessages()).extracting(OpenAiChatMessage::content)
                .containsExactly("prior question", "prior answer", "complete the task");
        assertThat(factory.capturedMessages()).noneMatch(message -> message.content().contains("system secret"));
    }

    @Test
    void springAiPromptMessagesPreserveInfrastructureRolesAndOrder() {
        List<Message> springAiMessages = OpenAiSpringAiModelFactory.toSpringAiMessages(List.of(
                OpenAiChatMessage.user("prior question"),
                OpenAiChatMessage.assistant("prior answer"),
                OpenAiChatMessage.user("current question")));

        assertThat(springAiMessages).hasSize(3);
        assertThat(springAiMessages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(springAiMessages.get(0).getText()).isEqualTo("prior question");
        assertThat(springAiMessages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(springAiMessages.get(1).getText()).isEqualTo("prior answer");
        assertThat(springAiMessages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(springAiMessages.get(2).getText()).isEqualTo("current question");
    }

    @Test
    void streamsTextToolUsageAndFinishChunksThroughPiContracts() {
        OpenAiCompatibleStreamingModelClient client = fakeClient(List.of(
                OpenAiStreamEvent.text("Hel"),
                OpenAiStreamEvent.text("lo"),
                OpenAiStreamEvent.toolCall(0, "call_1", "lookup", "{\"query\":\"pi\"}"),
                OpenAiStreamEvent.usage(new ModelUsage(8, 5, 13)),
                OpenAiStreamEvent.finish("tool_calls", null)));

        List<ModelStreamChunk> chunks = new ArrayList<>();
        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).isInstanceOf(ModelStreamChunk.TextDelta.class);
        assertThat(((ModelStreamChunk.TextDelta) chunks.get(0)).textDelta()).isEqualTo("Hel");
        assertThat(chunks.get(2)).isInstanceOf(ModelStreamChunk.ToolCallIntent.class);
        assertThat(((ModelStreamChunk.ToolCallIntent) chunks.get(2)).toolCall().arguments()).containsEntry("query", "pi");
        assertThat(chunks.get(3)).isInstanceOf(ModelStreamChunk.Usage.class);
        assertThat(chunks.get(4)).isInstanceOf(ModelStreamChunk.Finished.class);
        assertThat(((ModelStreamChunk.Finished) chunks.get(4)).finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
    }

    @Test
    void queueBackedSpringAiBridgeYieldsFirstChunkBeforeFluxCompletes() throws Exception {
        Sinks.Many<OpenAiStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        OpenAiSpringAiModelFactory.QueueBackedOpenAiStreamIterator iterator = new OpenAiSpringAiModelFactory.QueueBackedOpenAiStreamIterator(
                sink.asFlux(), new CancellationToken());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<OpenAiStreamEvent> first = executor.submit(iterator::next);

            sink.tryEmitNext(OpenAiStreamEvent.text("Hel"));

            assertThat(first.get(1, TimeUnit.SECONDS)).isEqualTo(OpenAiStreamEvent.text("Hel"));
        } finally {
            sink.tryEmitComplete();
            iterator.close();
            executor.shutdownNow();
        }
    }

    @Test
    void queueBackedSpringAiBridgeCloseCancelsUpstreamSubscription() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Flux<OpenAiStreamEvent> flux = Flux.create(sink -> {
            sink.onCancel(() -> cancelled.set(true));
            sink.next(OpenAiStreamEvent.text("A"));
        });
        OpenAiSpringAiModelFactory.QueueBackedOpenAiStreamIterator iterator = new OpenAiSpringAiModelFactory.QueueBackedOpenAiStreamIterator(
                flux, new CancellationToken());

        assertThat(iterator.next()).isEqualTo(OpenAiStreamEvent.text("A"));
        iterator.close();

        assertThat(cancelled).isTrue();
    }

    @Test
    void clientClosesStreamIteratorWhenSinkThrows() {
        AtomicBoolean closed = new AtomicBoolean(false);
        OpenAiCompatibleStreamingModelClient client = new OpenAiCompatibleStreamingModelClient(
                providerProperties(), secretResolver("sk-secret-value"), config -> (messages, cancellationToken) -> () -> new AutoCloseableIterator(closed));

        assertThatThrownBy(() -> client.stream(request(), new CancellationToken(), chunk -> {
            throw new IllegalStateException("sink failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(closed).isTrue();
    }

    @Test
    void cancellationStopsUpstreamAndEmitsCancelledChunk() {
        CancellationToken token = new CancellationToken();
        List<ModelStreamChunk> chunks = new ArrayList<>();
        OpenAiCompatibleStreamingModelClient client = fakeClient(List.of(
                OpenAiStreamEvent.text("before"),
                OpenAiStreamEvent.onSubscribe(() -> token.cancel("user requested")),
                OpenAiStreamEvent.text("after")));

        client.stream(request(), token, chunks::add);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1)).isInstanceOf(ModelStreamChunk.Cancelled.class);
        assertThat(((ModelStreamChunk.Cancelled) chunks.get(1)).reason()).contains("user requested");
    }

    @Test
    void timeoutAndProviderErrorsAreNormalizedAndSanitized() {
        OpenAiCompatibleStreamingModelClient client = fakeClient(List.of(OpenAiStreamEvent.error(new RuntimeException("Bearer sk-secret-value failed"))));

        List<ModelStreamChunk> chunks = new ArrayList<>();
        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(chunks).singleElement().isInstanceOf(ModelStreamChunk.ProviderError.class);
        ModelStreamChunk.ProviderError error = (ModelStreamChunk.ProviderError) chunks.getFirst();
        assertThat(error.errorSummary().safeMessage()).doesNotContain("sk-secret-value").doesNotContain("Bearer");
    }

    private static OpenAiCompatibleStreamingModelClient fakeClient(List<OpenAiStreamEvent> events) {
        return new OpenAiCompatibleStreamingModelClient(providerProperties(), secretResolver("sk-secret-value"),
                new CapturingSpringAiModelFactory(events));
    }

    private static SecretResolver secretResolver(String secret) {
        return ref -> Optional.of(ResolvedSecret.sensitive(ref, secret));
    }

    private static OpenAiProviderProperties providerProperties() {
        return OpenAiProviderProperties.openAiCompatible(
                "openai-compatible",
                "https://example.test/v1",
                "/chat/completions",
                "gpt-test",
                CREDENTIAL_REF,
                Map.of("temperature", "0.2"),
                Map.of("custom", "value"),
                new io.github.pi_java.agent.domain.model.ModelCapabilities(true, true,
                        io.github.pi_java.agent.domain.model.ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                OpenAiProviderProperties.ResilienceOptions.defaults());
    }

    private static ModelRequest request() {
        return requestWithHistory(new RunInput.ChatInput("hello"), List.of());
    }

    private static ModelRequest requestWithHistory(RunInput input) {
        return requestWithHistory(input, List.of(
                new SessionEntryPayload.MessageEntry("user", "prior question"),
                new SessionEntryPayload.MessageEntry("assistant", "prior answer"),
                new SessionEntryPayload.MessageEntry("system", "system secret"),
                new SessionEntryPayload.MessageEntry("", "blank role"),
                new SessionEntryPayload.MessageEntry("tool", "tool output")));
    }

    private static ModelRequest requestWithHistory(RunInput input, List<SessionEntryPayload.MessageEntry> messages) {
        AgentDefinition agent = new AgentDefinition(new AgentId("agent"), "Agent", "You help", "openai-compatible:gpt-test",
                Set.of("tools"), Set.of("policy"), new RuntimeLimits(Duration.ofSeconds(30), 4, 4),
                Set.of(InteractionMode.CHAT), "workspace", "output");
        WorkspaceScope workspace = new WorkspaceScope("tenant", "user", "session", "run-openai", "workspace", Set.of(), Set.of());
        RunContext context = new RunContext(agent, input,
                new SessionContext(messages, List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, agent.runtimeLimits(), new CancellationToken(), "trace", Instant.now());
        return new ModelRequest(context, List.of());
    }

    private static final class CapturingSpringAiModelFactory implements OpenAiSpringAiModelFactory {
        private final List<OpenAiStreamEvent> events;
        private OpenAiSpringAiModelFactory.ModelConfig captured;
        private List<OpenAiChatMessage> capturedMessages = List.of();

        private CapturingSpringAiModelFactory(List<OpenAiStreamEvent> events) {
            this.events = events;
        }

        @Override
        public OpenAiStreamSource create(ModelConfig config) {
            this.captured = config;
            return (messages, cancellationToken) -> {
                this.capturedMessages = List.copyOf(messages);
                return events;
            };
        }

        private ModelConfig captured() {
            return captured;
        }

        private List<OpenAiChatMessage> capturedMessages() {
            return capturedMessages;
        }
    }

    private static final class AutoCloseableIterator implements Iterator<OpenAiStreamEvent>, AutoCloseable {
        private final AtomicBoolean closed;
        private boolean consumed;

        private AutoCloseableIterator(AtomicBoolean closed) {
            this.closed = closed;
        }

        @Override
        public boolean hasNext() {
            return !consumed;
        }

        @Override
        public OpenAiStreamEvent next() {
            consumed = true;
            return OpenAiStreamEvent.text("boom");
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
