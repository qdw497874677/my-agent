package io.github.pi_java.agent.adapter.web.provider;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
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
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.ModelCapabilities;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiCompatibleStreamingModelClient;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiProviderProperties;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiSpringAiModelFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
@Profile("local")
public class DynamicAgentRuntime implements AgentRuntime {

    private final ProviderConfigStore configStore;
    private final EventSink eventSink;
    private final ToolExecutionGateway toolExecutionGateway;

    private volatile StreamingModelClient cachedClient;
    private volatile long cachedVersion = -1;

    public DynamicAgentRuntime(ProviderConfigStore configStore, EventSink eventSink,
                               ToolExecutionGateway toolExecutionGateway) {
        this.configStore = configStore;
        this.eventSink = eventSink;
        this.toolExecutionGateway = toolExecutionGateway;
    }

    @Override
    public RunHandle start(RunContext context) {
        ProviderConfig config = configStore.current();
        if (!config.isReady()) {
            return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
        }
        StreamingModelClient client = resolveClient(config);
        AgentDefinition definition = context.agentDefinition();
        ProviderModelRef.parse(definition.modelRef());
        client.stream(new ModelRequest(context, List.of()),
                context.cancellationToken(), new ModelDeltaPublishingSink(context, eventSink));
        return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
    }

    @Override
    public void cancel(String runId, String reason) {
    }

    private StreamingModelClient resolveClient(ProviderConfig config) {
        long version = configStore.version();
        if (cachedClient != null && version == cachedVersion) {
            return cachedClient;
        }
        synchronized (this) {
            if (cachedClient != null && configStore.version() == cachedVersion) {
                return cachedClient;
            }
            OpenAiProviderProperties props = OpenAiProviderProperties.openAiCompatible(
                    config.providerId(),
                    config.baseUrl(),
                    config.completionsPath(),
                    config.modelId(),
                    CredentialRef.of("config:dynamic"),
                    Map.of(),
                    Map.of(),
                    new ModelCapabilities(true, true, ModelCapabilities.UsageReporting.OPTIONAL, 128_000, 4_096, true),
                    OpenAiProviderProperties.ResilienceOptions.defaults());
            SecretResolver resolver = new StaticSecretResolver(config.apiKey());
            OpenAiSpringAiModelFactory factory = OpenAiSpringAiModelFactory.springAi();
            cachedClient = new OpenAiCompatibleStreamingModelClient(props, resolver, factory);
            cachedVersion = version;
            return cachedClient;
        }
    }

    private record StaticSecretResolver(String apiKey) implements SecretResolver {
        @Override
        public Optional<ResolvedSecret> resolve(SecretRef secretRef) {
            return Optional.of(new ResolvedSecret(secretRef, apiKey, "****"));
        }
    }

    static final class ModelDeltaPublishingSink implements StreamingModelClient.ModelStreamSink {
        private final RunContext context;
        private final EventSink eventSink;
        private final AtomicLong sequence = new AtomicLong(1);

        ModelDeltaPublishingSink(RunContext context, EventSink eventSink) {
            this.context = context;
            this.eventSink = eventSink;
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
