package io.github.pi_java.agent.adapter.web.provider;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
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
import io.github.pi_java.agent.domain.runtime.RunInput;
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
        ModelDeltaPublishingSink sink = new ModelDeltaPublishingSink(context, eventSink);
        ProviderConfig config = configStore.current();
        RunProviderMetadata runModel = runModelSnapshot(context);
        if (!config.isReady()) {
            sink.publishText(
                    "我已收到：" + inputText(context) + "\n\n当前还没有配置可用模型提供商。请在“管理治理 → 提供商”里配置 base URL、API Key 和模型，配置后我会调用真实模型回复。",
                    firstText(runModel.resolvedProviderId(), "local-dev"),
                    firstText(runModel.resolvedModelId(), "not-configured"),
                    firstText(runModel.selectedModelRef(), runModel.requestedModelRef(), "local-dev:not-configured"));
            return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
        }
        String providerId = firstText(runModel.resolvedProviderId(), providerFrom(context.agentDefinition().modelRef()), config.providerId());
        String modelId = firstText(runModel.resolvedModelId(), modelFrom(context.agentDefinition().modelRef()), config.modelId());
        String modelRef = firstText(runModel.selectedModelRef(), runModel.requestedModelRef(), providerId + ":" + modelId);
        sink.publishText("收到，我正在调用 " + modelId + " 处理。\n\n", providerId, modelId, modelRef);
        StreamingModelClient client = resolveClient(config);
        AgentDefinition definition = context.agentDefinition();
        ProviderModelRef.parse(definition.modelRef());
        client.stream(new ModelRequest(context, List.of()), context.cancellationToken(), sink);
        return new RunHandle(context.workspaceScope().runId(), RunStatus.SUCCEEDED, Optional.empty());
    }

    private static String inputText(RunContext context) {
        RunInput input = context.input();
        if (input instanceof RunInput.ChatInput chatInput) {
            return chatInput.text();
        }
        if (input instanceof RunInput.TaskInput taskInput) {
            return taskInput.objective();
        }
        if (input instanceof RunInput.WorkflowPlannerInput workflowPlannerInput) {
            return workflowPlannerInput.planRequest();
        }
        return String.valueOf(input);
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

    private static RunProviderMetadata runModelSnapshot(RunContext context) {
        ProviderModelRef parsed = ProviderModelRef.parse(context.agentDefinition().modelRef());
        return new RunProviderMetadata(context.agentDefinition().modelRef(), context.agentDefinition().modelRef(),
                parsed.providerId(), parsed.modelId(), null, null, null);
    }

    private static String providerFrom(String modelRef) {
        return ProviderModelRef.parse(modelRef).providerId();
    }

    private static String modelFrom(String modelRef) {
        return ProviderModelRef.parse(modelRef).modelId();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
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
            } else if (chunk instanceof ModelStreamChunk.ProviderError providerError) {
                publishText("模型调用失败：" + providerError.errorSummary().safeMessage(),
                        providerError.providerId(), providerError.modelId(), providerError.modelRef());
            }
        }

        void publishText(String text, String providerId, String modelId, String modelRef) {
            publish(new RunEventPayload.ModelDeltaPayload(modelRef, text, providerId, modelId, null, null, null));
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
