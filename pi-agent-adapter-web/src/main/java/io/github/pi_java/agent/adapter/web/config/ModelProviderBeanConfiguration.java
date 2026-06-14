package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.infrastructure.model.openai.EnvironmentAndPropertySecretResolver;
import io.github.pi_java.agent.infrastructure.model.openai.InMemoryModelProviderRegistry;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiCompatibleStreamingModelClient;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiProviderProperties;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiSpringAiModelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class ModelProviderBeanConfiguration {

    private static final String PROVIDER_PREFIX = "pi.providers.openai-compatible";
    private static final String API_KEY_PROPERTY = PROVIDER_PREFIX + ".api-key";

    @Bean
    OpenAiProviderProperties openAiProviderProperties(
            @Value("${pi.providers.openai-compatible.provider-id:openai-compatible}") String providerId,
            @Value("${pi.providers.openai-compatible.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${pi.providers.openai-compatible.completions-path:/chat/completions}") String completionsPath,
            @Value("${pi.providers.openai-compatible.default-model-id:gpt-4.1-mini}") String defaultModelId,
            @Value("${pi.providers.openai-compatible.credential-ref:config:pi.providers.openai-compatible.api-key}") String credentialRef) {
        return OpenAiProviderProperties.openAiCompatible(
                providerId,
                baseUrl,
                completionsPath,
                defaultModelId,
                CredentialRef.of(credentialRef),
                Map.of(),
                Map.of(),
                new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                OpenAiProviderProperties.ResilienceOptions.defaults());
    }

    @Bean
    ModelProviderRegistry modelProviderRegistry(OpenAiProviderProperties openAiProviderProperties) {
        return InMemoryModelProviderRegistry.fromProperties(List.of(openAiProviderProperties));
    }

    @Bean
    SecretResolver secretResolver(Environment environment) {
        return new EnvironmentAndPropertySecretResolver(System.getenv(), safeConfigurationProperties(environment));
    }

    @Bean
    @ConditionalOnProperty(prefix = "pi.providers.openai-compatible", name = "enabled", havingValue = "true")
    StreamingModelClient openAiCompatibleStreamingModelClient(OpenAiProviderProperties properties, SecretResolver secretResolver) {
        return new OpenAiCompatibleStreamingModelClient(properties, secretResolver, OpenAiSpringAiModelFactory.springAi());
    }

    @Bean
    @ConditionalOnMissingBean(AgentRuntime.class)
    @ConditionalOnProperty(prefix = "pi.providers.openai-compatible", name = "enabled", havingValue = "true")
    AgentRuntime openAiCompatibleAgentRuntime(StreamingModelClient streamingModelClient, EventSink eventSink) {
        return new StreamingOnlyAgentRuntime(streamingModelClient, eventSink);
    }

    private Map<String, String> safeConfigurationProperties(Environment environment) {
        String apiKey = environment.getProperty(API_KEY_PROPERTY);
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of();
        }
        return Map.of(API_KEY_PROPERTY, apiKey);
    }

    private static final class StreamingOnlyAgentRuntime implements AgentRuntime {
        private final StreamingModelClient streamingModelClient;
        private final EventSink eventSink;

        private StreamingOnlyAgentRuntime(StreamingModelClient streamingModelClient, EventSink eventSink) {
            this.streamingModelClient = streamingModelClient;
            this.eventSink = eventSink;
        }

        @Override
        public RunHandle start(RunContext context) {
            AgentDefinition definition = context.agentDefinition();
            ProviderModelRef.parse(definition.modelRef());
            streamingModelClient.stream(new io.github.pi_java.agent.domain.model.ModelRequest(context, List.of()),
                    context.cancellationToken(), chunk -> {
                    });
            return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
        }

        @Override
        public void cancel(String runId, String reason) {
            // Cancellation is observed through the RunContext token by the streaming client.
        }

        @Override
        public String toString() {
            return "StreamingOnlyAgentRuntime[eventSink=" + eventSink.getClass().getSimpleName() + "]";
        }
    }
}
