package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiCompatibleStreamingModelClient;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiProviderProperties;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiSpringAiModelFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OptionalRealProviderSmokeTest {

    @Test
    void realProviderSmokeIsSkippedUnlessExplicitlyEnabledAndConfigured() {
        SmokeConfig config = smokeConfig();

        List<ModelStreamChunk> chunks = new ArrayList<>();
        client(config.apiKey()).stream(request(config.model()), new CancellationToken(), chunks::add);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).noneSatisfy(chunk -> assertThat(chunk).isInstanceOf(ModelStreamChunk.ProviderError.class));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).isInstanceOfAny(ModelStreamChunk.TextDelta.class,
                ModelStreamChunk.Finished.class));
        assertThat(chunks.toString()).doesNotContain(config.apiKey());
    }

    @Test
    void realProviderWrongApiKeyReturnsAuthenticationError() {
        SmokeConfig config = smokeConfig();
        assumeTrue("true".equalsIgnoreCase(System.getenv("PI_OPENAI_COMPATIBLE_NEGATIVE_ENABLED")),
                "Set PI_OPENAI_COMPATIBLE_NEGATIVE_ENABLED=true to run real provider negative smoke");

        String wrongApiKey = config.apiKey() + "-invalid";
        List<ModelStreamChunk> chunks = new ArrayList<>();
        client(wrongApiKey).stream(request(config.model()), new CancellationToken(), chunks::add);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).isInstanceOfSatisfying(ModelStreamChunk.ProviderError.class,
                error -> {
                    assertThat(error.errorSummary().providerCode()).isEqualTo("provider_authentication_failed");
                    assertThat(error.errorSummary().httpStatus()).isIn(401, 403);
                    assertThat(error.errorSummary().userActionRequired()).isTrue();
                    assertThat(error.errorSummary().safeMessage()).doesNotContain(config.apiKey()).doesNotContain(wrongApiKey);
                }));
        assertThat(chunks.toString()).doesNotContain(config.apiKey()).doesNotContain(wrongApiKey);
    }

    private OpenAiCompatibleStreamingModelClient client(String apiKey) {
        SmokeConfig config = smokeConfig();
        OpenAiProviderProperties properties = OpenAiProviderProperties.openAiCompatible(
                "openai-compatible",
                config.baseUrl(),
                "/chat/completions",
                config.model(),
                new CredentialRef(SecretRef.of("env:PI_OPENAI_COMPATIBLE_API_KEY")),
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, false, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                OpenAiProviderProperties.ResilienceOptions.defaults());
        return new OpenAiCompatibleStreamingModelClient(properties,
                ref -> Optional.of(ResolvedSecret.sensitive(ref, apiKey)), OpenAiSpringAiModelFactory.springAi());
    }

    private static SmokeConfig smokeConfig() {
        String enabled = System.getenv("PI_OPENAI_COMPATIBLE_SMOKE_ENABLED");
        String baseUrl = System.getenv("PI_OPENAI_COMPATIBLE_BASE_URL");
        String apiKey = System.getenv("PI_OPENAI_COMPATIBLE_API_KEY");
        String model = System.getenv("PI_OPENAI_COMPATIBLE_MODEL");
        assumeTrue("true".equalsIgnoreCase(enabled), "Set PI_OPENAI_COMPATIBLE_SMOKE_ENABLED=true to run real provider smoke");
        assumeTrue(hasText(baseUrl) && hasText(apiKey) && hasText(model),
                "Set PI_OPENAI_COMPATIBLE_BASE_URL, PI_OPENAI_COMPATIBLE_API_KEY, and PI_OPENAI_COMPATIBLE_MODEL");
        return new SmokeConfig(baseUrl, apiKey, model);
    }

    private static ModelRequest request(String model) {
        AgentDefinition agent = new AgentDefinition(new AgentId("real-smoke-agent"), "Real Smoke Agent", "Answer briefly",
                "openai-compatible:" + model, Set.of(), Set.of("default"), new RuntimeLimits(Duration.ofSeconds(30), 1, 1),
                Set.of(InteractionMode.CHAT), "workspace", "output");
        WorkspaceScope workspace = new WorkspaceScope("tenant-smoke", "user-smoke", "session-smoke", "run-smoke", "workspace-smoke", Set.of(), Set.of());
        RunContext context = new RunContext(agent, new RunInput.ChatInput("Say pong."),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, agent.runtimeLimits(), new CancellationToken(), "trace-smoke", Instant.now());
        return new ModelRequest(context, List.of());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SmokeConfig(String baseUrl, String apiKey, String model) {
    }
}
