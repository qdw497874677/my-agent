package io.github.pi_java.agent.infrastructure.model.openai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpenAiCompatibleHttpInterfaceIntegrationTest {

    private static final String SECRET = "sk-http-secret";
    private static final CredentialRef CREDENTIAL_REF = new CredentialRef(SecretRef.of("env:PI_OPENAI_COMPATIBLE_API_KEY"));

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    @Test
    void streamingSuccessEmitsTextDeltasUsageAndStopFinish() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(successSseBody())));

        List<ModelStreamChunk> chunks = stream(providerProperties(wireMock.baseUrl() + "/v1"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.toString()).doesNotContain(SECRET));
        assertThat(chunks).extracting(ModelStreamChunk::sequence)
                .containsExactlyElementsOf(java.util.stream.LongStream.range(0, chunks.size()).boxed().toList());
        assertThat(chunks).filteredOn(ModelStreamChunk.TextDelta.class::isInstance)
                .extracting(chunk -> ((ModelStreamChunk.TextDelta) chunk).textDelta())
                .asString()
                .contains("Hel")
                .contains("lo");
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).isInstanceOfSatisfying(ModelStreamChunk.Finished.class,
                finished -> assertThat(finished.finishReason()).isEqualTo(ModelFinishReason.STOP)));
        assertThat(chunks).noneSatisfy(chunk -> assertThat(chunk).isInstanceOf(ModelStreamChunk.ProviderError.class));
    }

    @Test
    void http401MapsToAuthenticationFailedErrorAndIsNotRetryable() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"bad Authorization: Bearer " + SECRET + "\"}}")));

        ModelStreamChunk.ProviderError error = onlyError(providerProperties(wireMock.baseUrl() + "/v1"));

        assertThat(error.errorSummary().providerCode()).isEqualTo("provider_authentication_failed");
        assertThat(error.errorSummary().httpStatus()).isEqualTo(401);
        assertThat(error.errorSummary().retryable()).isFalse();
        assertThat(error.errorSummary().userActionRequired()).isTrue();
        assertSecretRedacted(error);
    }

    @Test
    void http429MapsToRateLimitedRetryableError() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"rate limited\"}}")));

        ModelStreamChunk.ProviderError error = onlyError(providerProperties(wireMock.baseUrl() + "/v1"));

        assertThat(error.errorSummary().providerCode()).isEqualTo("provider_rate_limited");
        assertThat(error.errorSummary().httpStatus()).isEqualTo(429);
        assertThat(error.errorSummary().retryable()).isTrue();
        assertSecretRedacted(error);
    }

    @Test
    void http5xxMapsToTransientFailureRetryableError() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"message\":\"upstream unavailable\"}}")));

        ModelStreamChunk.ProviderError error = onlyError(providerProperties(wireMock.baseUrl() + "/v1"));

        assertThat(error.errorSummary().providerCode()).isEqualTo("provider_transient_failure");
        assertThat(error.errorSummary().httpStatus()).isEqualTo(503);
        assertThat(error.errorSummary().retryable()).isTrue();
        assertSecretRedacted(error);
    }

    @Test
    void unreachableBaseUrlMapsToProviderErrorWithoutLeakingSecret() throws IOException {
        int port = freePort();

        ModelStreamChunk.ProviderError error = onlyError(providerProperties("http://127.0.0.1:" + port + "/v1"));

        assertThat(error.errorSummary().providerCode()).isIn("provider_transient_failure", "provider_timeout");
        assertSecretRedacted(error);
    }

    private static ModelStreamChunk.ProviderError onlyError(OpenAiProviderProperties properties) {
        List<ModelStreamChunk> chunks = stream(properties);
        assertThat(chunks).singleElement().isInstanceOf(ModelStreamChunk.ProviderError.class);
        return (ModelStreamChunk.ProviderError) chunks.getFirst();
    }

    private static List<ModelStreamChunk> stream(OpenAiProviderProperties properties) {
        OpenAiCompatibleStreamingModelClient client = new OpenAiCompatibleStreamingModelClient(properties,
                secretResolver(), OpenAiSpringAiModelFactory.springAi());
        List<ModelStreamChunk> chunks = new ArrayList<>();
        client.stream(request(), new CancellationToken(), chunks::add);
        return chunks;
    }

    private static SecretResolver secretResolver() {
        return ref -> Optional.of(ResolvedSecret.sensitive(ref, SECRET));
    }

    private static OpenAiProviderProperties providerProperties(String baseUrl) {
        return OpenAiProviderProperties.openAiCompatible(
                "openai-compatible",
                baseUrl,
                "/chat/completions",
                "gpt-http",
                CREDENTIAL_REF,
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                OpenAiProviderProperties.ResilienceOptions.defaults());
    }

    private static ModelRequest request() {
        AgentDefinition agent = new AgentDefinition(new AgentId("agent-http"), "HTTP Agent", "Contract agent",
                "openai-compatible:gpt-http", Set.of(), Set.of("policy"),
                new RuntimeLimits(Duration.ofSeconds(30), 4, 4), Set.of(InteractionMode.CHAT),
                "workspace-policy", "output-policy");
        WorkspaceScope workspace = new WorkspaceScope("tenant", "user", "session", "run-http", "workspace", Set.of(), Set.of());
        RunContext context = new RunContext(agent, new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, agent.runtimeLimits(), new CancellationToken(), "trace-http", Instant.now());
        return new ModelRequest(context, List.of());
    }

    private static void assertSecretRedacted(ModelStreamChunk.ProviderError error) {
        assertThat(error.errorSummary().safeMessage())
                .doesNotContain(SECRET)
                .doesNotContain("Bearer")
                .doesNotContain("api_key")
                .doesNotContain("authorization");
        assertThat(error.toString()).doesNotContain(SECRET).doesNotContain("Bearer " + SECRET);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String successSseBody() {
        return """
                data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"gpt-http","choices":[{"index":0,"delta":{"role":"assistant","content":"Hel"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"gpt-http","choices":[{"index":0,"delta":{"content":"lo"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"gpt-http","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}

                data: [DONE]

                """;
    }
}
