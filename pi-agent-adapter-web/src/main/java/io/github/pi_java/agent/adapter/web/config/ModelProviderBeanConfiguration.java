package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
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
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionModelProviderRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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
    ModelProviderRegistry modelProviderRegistry(OpenAiProviderProperties openAiProviderProperties,
                                                Optional<DefaultExtensionContributionRegistry> extensionContributions) {
        ModelProviderRegistry builtins = InMemoryModelProviderRegistry.fromProperties(List.of(openAiProviderProperties));
        return extensionContributions
                .<ModelProviderRegistry>map(contributions -> new CompositeModelProviderRegistry(
                        List.of(builtins, new ExtensionModelProviderRegistry(contributions))))
                .orElse(builtins);
    }

    @Bean
    SecretResolver secretResolver(Environment environment) {
        return new EnvironmentAndPropertySecretResolver(System.getenv(), safeConfigurationProperties(environment));
    }

    @Bean
    @ConditionalOnProperty(prefix = "pi.providers.openai-compatible", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(OpenAiSpringAiModelFactory.class)
    OpenAiSpringAiModelFactory openAiSpringAiModelFactory() {
        return OpenAiSpringAiModelFactory.springAi();
    }

    @Bean
    @ConditionalOnProperty(prefix = "pi.providers.openai-compatible", name = "enabled", havingValue = "true")
    StreamingModelClient openAiCompatibleStreamingModelClient(OpenAiProviderProperties properties, SecretResolver secretResolver,
                                                             OpenAiSpringAiModelFactory openAiSpringAiModelFactory) {
        return new OpenAiCompatibleStreamingModelClient(properties, secretResolver, openAiSpringAiModelFactory);
    }

    @Bean
    @ConditionalOnMissingBean(AgentRuntime.class)
    @ConditionalOnProperty(prefix = "pi.providers.openai-compatible", name = "enabled", havingValue = "true")
    AgentRuntime openAiCompatibleAgentRuntime(StreamingModelClient streamingModelClient, EventSink eventSink,
                                             ToolExecutionGateway toolExecutionGateway) {
        return new StreamingOnlyAgentRuntime(streamingModelClient, eventSink, toolExecutionGateway);
    }

    private Map<String, String> safeConfigurationProperties(Environment environment) {
        String apiKey = environment.getProperty(API_KEY_PROPERTY);
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of();
        }
        return Map.of(API_KEY_PROPERTY, apiKey);
    }

    private record CompositeModelProviderRegistry(List<ModelProviderRegistry> registries) implements ModelProviderRegistry {
        private CompositeModelProviderRegistry {
            registries = List.copyOf(registries);
        }

        @Override
        public List<io.github.pi_java.agent.domain.model.ProviderDescriptor> listProviders() {
            Map<String, io.github.pi_java.agent.domain.model.ProviderDescriptor> providers = new LinkedHashMap<>();
            for (ModelProviderRegistry registry : registries) {
                for (io.github.pi_java.agent.domain.model.ProviderDescriptor provider : registry.listProviders()) {
                    providers.putIfAbsent(provider.providerId(), provider);
                }
            }
            return List.copyOf(providers.values());
        }
    }

    private static final class StreamingOnlyAgentRuntime implements AgentRuntime {
        private final StreamingModelClient streamingModelClient;
        private final EventSink eventSink;
        private final ToolExecutionGateway toolExecutionGateway;

        private StreamingOnlyAgentRuntime(StreamingModelClient streamingModelClient, EventSink eventSink,
                                          ToolExecutionGateway toolExecutionGateway) {
            this.streamingModelClient = streamingModelClient;
            this.eventSink = eventSink;
            this.toolExecutionGateway = toolExecutionGateway;
        }

        @Override
        public RunHandle start(RunContext context) {
            AgentDefinition definition = context.agentDefinition();
            ProviderModelRef.parse(definition.modelRef());
            streamingModelClient.stream(new io.github.pi_java.agent.domain.model.ModelRequest(context, List.of()),
                    context.cancellationToken(), new ModelDeltaPublishingSink(context));
            return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
        }

        @Override
        public void cancel(String runId, String reason) {
            // Cancellation is observed through the RunContext token by the streaming client.
        }

        @Override
        public String toString() {
            return "StreamingOnlyAgentRuntime[eventSink=" + eventSink.getClass().getSimpleName()
                    + ", ToolExecutionGateway=" + toolExecutionGateway.getClass().getSimpleName() + "]";
        }

        private final class ModelDeltaPublishingSink implements StreamingModelClient.ModelStreamSink {
            private final RunContext context;
            private final AtomicLong sequence = new AtomicLong(1);

            private ModelDeltaPublishingSink(RunContext context) {
                this.context = context;
            }

            @Override
            public void accept(ModelStreamChunk chunk) {
                if (chunk instanceof ModelStreamChunk.TextDelta textDelta) {
                    publish(new RunEventPayload.ModelDeltaPayload(textDelta.modelRef(), textDelta.textDelta(),
                            textDelta.providerId(), textDelta.modelId(), null, null, textDelta.latency()));
                } else if (chunk instanceof ModelStreamChunk.Finished finished) {
                    publish(new RunEventPayload.ModelDeltaPayload(finished.modelRef(), "", finished.providerId(),
                            finished.modelId(), finished.finishReason(), finished.usage(), finished.latency()));
                }
            }

            private void publish(RunEventPayload.ModelDeltaPayload payload) {
                eventSink.publish(new RunEvent(
                        "evt-" + UUID.randomUUID(),
                        new TenantId(context.workspaceScope().tenantId()),
                        new UserId(context.workspaceScope().userId()),
                        new SessionId(context.workspaceScope().sessionId()),
                        new RunId(context.workspaceScope().runId()),
                        new StepId("model-step"),
                        new WorkspaceId(context.workspaceScope().workspaceId()),
                        sequence.getAndIncrement(),
                        java.time.Instant.now(),
                        RunEventType.MODEL_DELTA,
                        new TraceId(context.traceId()),
                        new CorrelationId(context.traceId()),
                        new CausationId("model-stream"),
                        payload,
                        EventVisibility.USER,
                        new RedactionMetadata(false, false, Set.of(), "provider-runtime")));
            }
        }
    }
}
