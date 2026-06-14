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
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        AgentDefinition agent = new AgentDefinition(new AgentId("agent"), "Agent", "You help", "openai-compatible:gpt-test",
                Set.of("tools"), Set.of("policy"), new RuntimeLimits(Duration.ofSeconds(30), 4, 4),
                Set.of(InteractionMode.CHAT), "workspace", "output");
        WorkspaceScope workspace = new WorkspaceScope("tenant", "user", "session", "run-openai", "workspace", Set.of(), Set.of());
        RunContext context = new RunContext(agent, new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, agent.runtimeLimits(), new CancellationToken(), "trace", Instant.now());
        return new ModelRequest(context, List.of());
    }

    private static final class CapturingSpringAiModelFactory implements OpenAiSpringAiModelFactory {
        private final List<OpenAiStreamEvent> events;
        private OpenAiSpringAiModelFactory.ModelConfig captured;

        private CapturingSpringAiModelFactory(List<OpenAiStreamEvent> events) {
            this.events = events;
        }

        @Override
        public OpenAiStreamSource create(ModelConfig config) {
            this.captured = config;
            return (prompt, cancellationToken) -> events;
        }

        private ModelConfig captured() {
            return captured;
        }
    }
}
