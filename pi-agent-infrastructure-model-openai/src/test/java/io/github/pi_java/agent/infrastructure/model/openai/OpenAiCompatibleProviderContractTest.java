package io.github.pi_java.agent.infrastructure.model.openai;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderContractTest {

    private static final String SECRET = "sk-contract-secret";
    private static final CredentialRef CREDENTIAL_REF = new CredentialRef(SecretRef.of("env:PI_OPENAI_COMPATIBLE_API_KEY"));

    @Test
    void streamsTextDeltaWithTerminalFinishReasonAndNonNegativeLatency() {
        List<ModelStreamChunk> chunks = stream(List.of(
                OpenAiStreamEvent.text("Hel"),
                OpenAiStreamEvent.text("lo"),
                OpenAiStreamEvent.finish("stop", new ModelUsage(2, 3, 5))));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(ModelStreamChunk::sequence).containsExactly(0L, 1L, 2L);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.providerId()).isEqualTo("openai-compatible");
            assertThat(chunk.modelId()).isEqualTo("gpt-contract");
            assertThat(chunk.modelRef()).isEqualTo("openai-compatible:gpt-contract");
            assertThat(chunk.latency()).isNotNull();
            assertThat(chunk.latency().isNegative()).isFalse();
        });
        assertThat(chunks.get(0)).isInstanceOfSatisfying(ModelStreamChunk.TextDelta.class,
                delta -> assertThat(delta.textDelta()).isEqualTo("Hel"));
        assertThat(chunks.get(2)).isInstanceOfSatisfying(ModelStreamChunk.Finished.class, finished -> {
            assertThat(finished.finishReason()).isEqualTo(ModelFinishReason.STOP);
            assertThat(finished.usage()).isEqualTo(new ModelUsage(2, 3, 5));
        });
    }

    @Test
    void aggregatesFragmentedToolCallArgumentsIntoOneCompleteIntent() {
        List<ModelStreamChunk> chunks = stream(List.of(
                OpenAiStreamEvent.toolCall(0, "call_1", "search", "{\"query\":"),
                OpenAiStreamEvent.toolCall(0, "call_1", null, "\"pi\",\"limit\":"),
                OpenAiStreamEvent.toolCall(0, "call_1", null, "3}"),
                OpenAiStreamEvent.finish("tool_calls", null)));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isInstanceOfSatisfying(ModelStreamChunk.ToolCallIntent.class, tool -> {
            assertThat(tool.toolCall().toolName()).isEqualTo("search");
            assertThat(tool.toolCall().arguments()).containsEntry("query", "pi").containsEntry("limit", 3.0);
        });
        assertThat(chunks.get(1)).isInstanceOfSatisfying(ModelStreamChunk.Finished.class,
                finished -> assertThat(finished.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS));
    }

    @Test
    void supportsUsagePresentAndMissingUsageCases() {
        List<ModelStreamChunk> withUsage = stream(List.of(
                OpenAiStreamEvent.usage(new ModelUsage(4, 6, 10)),
                OpenAiStreamEvent.finish("stop", null)));
        assertThat(withUsage).anySatisfy(chunk -> assertThat(chunk).isInstanceOf(ModelStreamChunk.Usage.class));

        List<ModelStreamChunk> missingUsage = stream(List.of(OpenAiStreamEvent.finish("stop", null)));
        assertThat(missingUsage).singleElement().isInstanceOfSatisfying(ModelStreamChunk.Finished.class,
                finished -> assertThat(finished.usage()).isNull());
    }

    @Test
    void maps429And5xxAsRetryableBeforeAnyEmittedChunk() {
        ModelStreamChunk.ProviderError rateLimited = onlyError(new ThrowingFactory(
                new ProviderHttpException(429, "rate limited " + SECRET)));
        assertThat(rateLimited.errorSummary().providerCode()).isEqualTo("provider_rate_limited");
        assertThat(rateLimited.errorSummary().httpStatus()).isEqualTo(429);
        assertThat(rateLimited.errorSummary().retryable()).isTrue();
        assertSecretRedacted(rateLimited);

        ModelStreamChunk.ProviderError transientFailure = onlyError(new ThrowingFactory(
                new ProviderHttpException(503, "upstream unavailable " + SECRET)));
        assertThat(transientFailure.errorSummary().providerCode()).isEqualTo("provider_transient_failure");
        assertThat(transientFailure.errorSummary().httpStatus()).isEqualTo(503);
        assertThat(transientFailure.errorSummary().retryable()).isTrue();
        assertSecretRedacted(transientFailure);
    }

    @Test
    void midStreamProviderErrorIsNotRetriedAfterOutputWasEmitted() {
        AtomicInteger sourceCreations = new AtomicInteger();
        OpenAiCompatibleStreamingModelClient client = client(new CapturingFactory(List.of(
                OpenAiStreamEvent.text("before"),
                OpenAiStreamEvent.error(new ProviderHttpException(503, "Authorization: Bearer " + SECRET))), sourceCreations));

        List<ModelStreamChunk> chunks = new ArrayList<>();
        client.stream(request(), new CancellationToken(), chunks::add);

        assertThat(sourceCreations).hasValue(1);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1)).isInstanceOfSatisfying(ModelStreamChunk.ProviderError.class, error -> {
            assertThat(error.errorSummary().providerCode()).isEqualTo("provider_stream_failed");
            assertThat(error.errorSummary().retryable()).isFalse();
            assertSecretRedacted(error);
        });
    }

    @Test
    void timeoutAndCancellationStopUpstreamStream() {
        List<ModelStreamChunk> timeout = stream(List.of(OpenAiStreamEvent.error(new RuntimeException("timeout after 50ms " + SECRET))));
        assertThat(timeout).singleElement().isInstanceOfSatisfying(ModelStreamChunk.ProviderError.class, error -> {
            assertThat(error.errorSummary().providerCode()).isEqualTo("provider_timeout");
            assertSecretRedacted(error);
        });

        CancellationToken token = new CancellationToken();
        List<ModelStreamChunk> cancelled = stream(List.of(
                OpenAiStreamEvent.text("before"),
                OpenAiStreamEvent.onSubscribe(() -> token.cancel("human cancelled")),
                OpenAiStreamEvent.text("after")), token);
        assertThat(cancelled).hasSize(2);
        assertThat(cancelled.get(1)).isInstanceOfSatisfying(ModelStreamChunk.Cancelled.class,
                chunk -> assertThat(chunk.reason()).isEqualTo("human cancelled"));
        assertThat(cancelled).noneSatisfy(chunk -> assertThat(chunk.toString()).contains("after"));
    }

    @Test
    void rawSecretValuesNeverAppearInChunksErrorsOrFactoryVisibleMessages() {
        List<ModelStreamChunk> chunks = stream(List.of(
                OpenAiStreamEvent.text("safe"),
                OpenAiStreamEvent.error(new RuntimeException("Bearer " + SECRET + " failed with api_key=" + SECRET))));

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.toString())
                .doesNotContain(SECRET)
                .doesNotContain("Bearer " + SECRET)
                .doesNotContain("api_key=" + SECRET));
        assertThat(chunks.get(1)).isInstanceOfSatisfying(ModelStreamChunk.ProviderError.class, this::assertSecretRedacted);
    }

    private static ModelStreamChunk.ProviderError onlyError(OpenAiSpringAiModelFactory factory) {
        List<ModelStreamChunk> chunks = new ArrayList<>();
        client(factory).stream(request(), new CancellationToken(), chunks::add);
        assertThat(chunks).singleElement().isInstanceOf(ModelStreamChunk.ProviderError.class);
        return (ModelStreamChunk.ProviderError) chunks.getFirst();
    }

    private static List<ModelStreamChunk> stream(List<OpenAiStreamEvent> events) {
        return stream(events, new CancellationToken());
    }

    private static List<ModelStreamChunk> stream(List<OpenAiStreamEvent> events, CancellationToken token) {
        List<ModelStreamChunk> chunks = new ArrayList<>();
        client(new CapturingFactory(events, new AtomicInteger())).stream(request(), token, chunks::add);
        return chunks;
    }

    private void assertSecretRedacted(ModelStreamChunk.ProviderError error) {
        assertThat(error.errorSummary().safeMessage())
                .doesNotContain(SECRET)
                .doesNotContain("Bearer")
                .doesNotContain("api_key")
                .doesNotContain("authorization");
        assertThat(error.toString()).doesNotContain(SECRET).doesNotContain("Bearer " + SECRET);
    }

    private static OpenAiCompatibleStreamingModelClient client(OpenAiSpringAiModelFactory factory) {
        return new OpenAiCompatibleStreamingModelClient(providerProperties(), secretResolver(), factory);
    }

    private static SecretResolver secretResolver() {
        return ref -> Optional.of(ResolvedSecret.sensitive(ref, SECRET));
    }

    private static OpenAiProviderProperties providerProperties() {
        return OpenAiProviderProperties.openAiCompatible(
                "openai-compatible",
                "https://example.test/v1",
                "/chat/completions",
                "gpt-contract",
                CREDENTIAL_REF,
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                OpenAiProviderProperties.ResilienceOptions.defaults());
    }

    private static ModelRequest request() {
        AgentDefinition agent = new AgentDefinition(new AgentId("agent"), "Agent", "Contract agent",
                "openai-compatible:gpt-contract", Set.of("search"), Set.of("policy"),
                new RuntimeLimits(Duration.ofSeconds(30), 4, 4), Set.of(InteractionMode.CHAT),
                "workspace-policy", "output-policy");
        WorkspaceScope workspace = new WorkspaceScope("tenant", "user", "session", "run-contract", "workspace", Set.of(), Set.of());
        RunContext context = new RunContext(agent, new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, agent.runtimeLimits(), new CancellationToken(), "trace-contract", Instant.now());
        return new ModelRequest(context, List.of());
    }

    private static final class CapturingFactory implements OpenAiSpringAiModelFactory {
        private final List<OpenAiStreamEvent> events;
        private final AtomicInteger sourceCreations;

        private CapturingFactory(List<OpenAiStreamEvent> events, AtomicInteger sourceCreations) {
            this.events = events;
            this.sourceCreations = sourceCreations;
        }

        @Override
        public OpenAiStreamSource create(ModelConfig config) {
            sourceCreations.incrementAndGet();
            return (prompt, cancellationToken) -> events;
        }
    }

    private static final class ThrowingFactory implements OpenAiSpringAiModelFactory {
        private final RuntimeException exception;

        private ThrowingFactory(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public OpenAiStreamSource create(ModelConfig config) {
            throw exception;
        }
    }

    private static final class ProviderHttpException extends RuntimeException {
        private final int status;

        private ProviderHttpException(int status, String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
