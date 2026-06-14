package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.common.IdGenerator;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.error.FailureSummary;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.model.ModelClient;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.model.ModelStreamChunk;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolInvoker;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GeneralAgentLoop implements AgentRuntime {
    private final ModelClient modelClient;
    private final StreamingModelClient streamingModelClient;
    private final ToolInvoker toolInvoker;
    private final FakePolicy policy;
    private final EventSink eventSink;
    private final IdGenerator ids;
    private final DeterministicClock clock;
    private long sequence;

    public GeneralAgentLoop(ModelClient modelClient, ToolInvoker toolInvoker, FakePolicy policy,
                            EventSink eventSink, IdGenerator ids, DeterministicClock clock) {
        this.modelClient = modelClient;
        this.streamingModelClient = null;
        this.toolInvoker = toolInvoker;
        this.policy = policy;
        this.eventSink = eventSink;
        this.ids = ids;
        this.clock = clock;
    }

    public GeneralAgentLoop(StreamingModelClient streamingModelClient, ToolInvoker toolInvoker, FakePolicy policy,
                            EventSink eventSink, IdGenerator ids, DeterministicClock clock) {
        this.modelClient = null;
        this.streamingModelClient = streamingModelClient;
        this.toolInvoker = toolInvoker;
        this.policy = policy;
        this.eventSink = eventSink;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public RunHandle start(RunContext context) {
        RunId runId = new RunId(context.workspaceScope().runId());
        List<ToolResult> toolResults = new ArrayList<>();
        publish(context, runId, stepId("run"), RunEventType.RUN_CREATED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.QUEUED, null));
        if (context.cancellationToken().isCancellationRequested()) {
            return cancelled(context, runId);
        }
        publish(context, runId, stepId("run"), RunEventType.RUN_STARTED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.RUNNING, null));

        for (int step = 1; step <= context.limits().maxSteps(); step++) {
            if (context.cancellationToken().isCancellationRequested()) {
                return cancelled(context, runId);
            }
            if (deadlineExpired(context)) {
                return failed(context, runId, PiError.Category.TIMEOUT, "deadline exceeded");
            }

            StepId stepId = stepId("step");
            publish(context, runId, stepId, RunEventType.MODEL_REQUESTED,
                    new RunEventPayload.StepLifecyclePayload(stepId, io.github.pi_java.agent.domain.runtime.StepStatus.RUNNING, "model"));
            if (streamingModelClient != null) {
                StepOutcome outcome = consumeStream(context, runId, stepId, new ModelRequest(context, toolResults));
                if (outcome.status() != StepStatus.CONTINUE) {
                    return outcome.handle();
                }
                if (outcome.toolCall() == null) {
                    publish(context, runId, stepId, RunEventType.RUN_COMPLETED,
                            new RunEventPayload.RunLifecyclePayload(RunStatus.SUCCEEDED, null));
                    return new RunHandle(runId.value(), RunStatus.SUCCEEDED, Optional.empty());
                }
                ToolOutcome toolOutcome = executeToolCall(context, runId, stepId, outcome.toolCall());
                if (toolOutcome.status() != StepStatus.CONTINUE) {
                    return toolOutcome.handle();
                }
                toolResults.add(toolOutcome.toolResult());
                continue;
            }
            ModelResponse response = modelClient.next(new ModelRequest(context, toolResults), context.cancellationToken());
            if (response instanceof ModelResponse.FinalText finalText) {
                publish(context, runId, stepId, RunEventType.MODEL_DELTA,
                        new RunEventPayload.ModelDeltaPayload(modelRef(context, finalText.modelRef()), finalText.text(),
                                finalText.providerId(), finalText.modelId(), finalText.finishReason(), finalText.usage(), finalText.latency()));
                publish(context, runId, stepId, RunEventType.RUN_COMPLETED,
                        new RunEventPayload.RunLifecyclePayload(RunStatus.SUCCEEDED, null));
                return new RunHandle(runId.value(), RunStatus.SUCCEEDED, Optional.empty());
            }
            ToolCall toolCall = ((ModelResponse.ToolCallIntent) response).toolCall();
            ToolOutcome toolOutcome = executeToolCall(context, runId, stepId, toolCall);
            if (toolOutcome.status() != StepStatus.CONTINUE) {
                return toolOutcome.handle();
            }
            toolResults.add(toolOutcome.toolResult());
        }
        return failed(context, runId, PiError.Category.RUNTIME, "max step budget exceeded");
    }

    private StepOutcome consumeStream(RunContext context, RunId runId, StepId stepId, ModelRequest request) {
        List<ModelStreamChunk> chunks = new ArrayList<>();
        try {
            streamingModelClient.stream(request, context.cancellationToken(), chunks::add);
        } catch (StreamingModelClient.ModelStreamException exception) {
            return StepOutcome.terminal(failedFromProviderError(context, runId, exception.errorSummary()));
        }

        ToolCall toolCall = null;
        ModelFinishReason finishReason = null;
        ModelUsage usage = null;
        java.time.Duration finishLatency = null;
        String finishProviderId = null;
        String finishModelId = null;
        String finishModelRef = null;
        for (ModelStreamChunk chunk : chunks) {
            if (chunk instanceof ModelStreamChunk.TextDelta textDelta) {
                publish(context, runId, stepId, RunEventType.MODEL_DELTA,
                        new RunEventPayload.ModelDeltaPayload(textDelta.modelRef(), textDelta.textDelta(), textDelta.providerId(),
                                textDelta.modelId(), null, null, textDelta.latency()));
            } else if (chunk instanceof ModelStreamChunk.ToolCallIntent intent) {
                toolCall = intent.toolCall();
            } else if (chunk instanceof ModelStreamChunk.Usage usageChunk) {
                usage = usageChunk.usage();
                finishLatency = usageChunk.latency();
                finishProviderId = usageChunk.providerId();
                finishModelId = usageChunk.modelId();
                finishModelRef = usageChunk.modelRef();
            } else if (chunk instanceof ModelStreamChunk.Finished finished) {
                finishReason = finished.finishReason();
                usage = finished.usage() == null ? usage : finished.usage();
                finishLatency = finished.latency();
                finishProviderId = finished.providerId();
                finishModelId = finished.modelId();
                finishModelRef = finished.modelRef();
            } else if (chunk instanceof ModelStreamChunk.Cancelled) {
                return StepOutcome.terminal(cancelled(context, runId));
            } else if (chunk instanceof ModelStreamChunk.TimedOut timedOut) {
                return StepOutcome.terminal(failed(context, runId, PiError.Category.TIMEOUT,
                        "model stream timed out after " + timedOut.timeout()));
            } else if (chunk instanceof ModelStreamChunk.ProviderError providerError) {
                return StepOutcome.terminal(failedFromProviderError(context, runId, providerError.errorSummary()));
            }
            if (context.cancellationToken().isCancellationRequested()) {
                return StepOutcome.terminal(cancelled(context, runId));
            }
        }
        if (finishReason != null || usage != null) {
            publish(context, runId, stepId, RunEventType.MODEL_DELTA,
                    new RunEventPayload.ModelDeltaPayload(modelRef(context, finishModelRef), "", finishProviderId, finishModelId,
                            finishReason, usage, finishLatency));
        }
        return StepOutcome.continueWith(toolCall);
    }

    private ToolOutcome executeToolCall(RunContext context, RunId runId, StepId stepId, ToolCall toolCall) {
            publish(context, runId, stepId, RunEventType.TOOL_PROPOSED,
                    new RunEventPayload.ToolProposedPayload(toolCall));
            PolicyDecision decision = policy.decide(toolCall);
            publish(context, runId, stepId, RunEventType.POLICY_DECIDED,
                    new RunEventPayload.PolicyDecidedPayload("fake-policy", decision.name(), "fake policy decision"));
            if (decision != PolicyDecision.ALLOW) {
                FailureSummary summary = failure(PiError.Category.POLICY, "policy blocked", false, false, true);
                publish(context, runId, stepId, RunEventType.RUN_POLICY_BLOCKED,
                        new RunEventPayload.RunLifecyclePayload(RunStatus.POLICY_BLOCKED, summary));
                return ToolOutcome.terminal(new RunHandle(runId.value(), RunStatus.POLICY_BLOCKED, Optional.of(summary)));
            }
            if (context.cancellationToken().isCancellationRequested()) {
                return ToolOutcome.terminal(cancelled(context, runId));
            }
            ToolResult toolResult = toolInvoker.invoke(toolCall, context, context.cancellationToken());
            publish(context, runId, stepId, RunEventType.TOOL_COMPLETED,
                    new RunEventPayload.ToolCompletedPayload(toolResult));
            return ToolOutcome.continueWith(toolResult);
    }

    @Override
    public void cancel(String runId, String reason) {
        // Testkit runs synchronously; callers cancel through the RunContext token.
    }

    private RunHandle cancelled(RunContext context, RunId runId) {
        FailureSummary summary = failure(PiError.Category.CANCELLATION,
                context.cancellationToken().reason().orElse("cancelled"), false, true, false);
        publish(context, runId, stepId("cancel"), RunEventType.RUN_CANCELLED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.CANCELLED, summary));
        return new RunHandle(runId.value(), RunStatus.CANCELLED, Optional.of(summary));
    }

    private RunHandle failed(RunContext context, RunId runId, PiError.Category category, String message) {
        FailureSummary summary = failure(category, message, false, true, false);
        publish(context, runId, stepId("failed"), RunEventType.RUN_FAILED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.FAILED, summary));
        return new RunHandle(runId.value(), RunStatus.FAILED, Optional.of(summary));
    }

    private RunHandle failedFromProviderError(RunContext context, RunId runId, ProviderErrorSummary errorSummary) {
        FailureSummary summary = new FailureSummary(errorSummary.safeMessage(), errorSummary.piError());
        publish(context, runId, stepId("failed"), RunEventType.RUN_FAILED,
                new RunEventPayload.RunLifecyclePayload(RunStatus.FAILED, summary));
        return new RunHandle(runId.value(), RunStatus.FAILED, Optional.of(summary));
    }

    private String modelRef(RunContext context, String streamedModelRef) {
        return streamedModelRef == null || streamedModelRef.isBlank() ? context.agentDefinition().modelRef() : streamedModelRef;
    }

    private FailureSummary failure(PiError.Category category, String message, boolean retryable,
                                   boolean recoverable, boolean userActionRequired) {
        return new FailureSummary(message, new PiError(category, "FAKE_GENERAL_AGENT_LOOP", PiError.Severity.ERROR,
                EventVisibility.USER, retryable, recoverable, userActionRequired));
    }

    private boolean deadlineExpired(RunContext context) {
        return !clock.peek().isBefore(context.startedAt().plus(context.limits().deadline()));
    }

    private StepId stepId(String prefix) {
        return new StepId(ids.nextId(prefix));
    }

    private void publish(RunContext context, RunId runId, StepId stepId, RunEventType type, RunEventPayload payload) {
        RunEvent event = new RunEvent(
                ids.nextId("event"),
                new TenantId(context.workspaceScope().tenantId()),
                new UserId(context.workspaceScope().userId()),
                new io.github.pi_java.agent.domain.common.PlatformIds.SessionId(context.workspaceScope().sessionId()),
                runId,
                stepId,
                new WorkspaceId(context.workspaceScope().workspaceId()),
                ++sequence,
                clock.nextInstant(),
                type,
                new TraceId(context.traceId()),
                new CorrelationId(context.traceId()),
                new CausationId(stepId.value()),
                payload,
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "fake-redaction"));
        eventSink.publish(event);
    }

    private enum StepStatus {
        CONTINUE,
        TERMINAL
    }

    private record StepOutcome(StepStatus status, ToolCall toolCall, RunHandle handle) {
        private static StepOutcome continueWith(ToolCall toolCall) {
            return new StepOutcome(StepStatus.CONTINUE, toolCall, null);
        }

        private static StepOutcome terminal(RunHandle handle) {
            return new StepOutcome(StepStatus.TERMINAL, null, handle);
        }
    }

    private record ToolOutcome(StepStatus status, ToolResult toolResult, RunHandle handle) {
        private static ToolOutcome continueWith(ToolResult toolResult) {
            return new ToolOutcome(StepStatus.CONTINUE, toolResult, null);
        }

        private static ToolOutcome terminal(RunHandle handle) {
            return new ToolOutcome(StepStatus.TERMINAL, null, handle);
        }
    }
}
