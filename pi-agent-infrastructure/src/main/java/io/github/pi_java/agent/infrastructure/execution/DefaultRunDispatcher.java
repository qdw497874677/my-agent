package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.usecase.ConversationContextAssembler;
import io.github.pi_java.agent.app.usecase.ConversationContextMetadata;
import io.github.pi_java.agent.app.usecase.ConversationContextPolicy;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultRunDispatcher implements RunDispatcher {

    private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "FAILED", "CANCELLED", "TIMED_OUT");

    private final RunQueue runQueue;
    private final RunProjectionRepository runProjectionRepository;
    private final RunEventStore runEventStore;
    private final RunTerminalEventPublisher runTerminalEventPublisher;
    private final CancellationRegistry cancellationRegistry;
    private final AuditRepository auditRepository;
    private final AgentRuntime agentRuntime;
    private final Clock clock;
    private final Duration runTimeout;
    private final AgentDefinition agentDefinition;
    private final RuntimeLimits runtimeLimits;
    private final ConversationContextAssembler conversationContextAssembler;

    public DefaultRunDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            Duration runTimeout) {
        this(runQueue, runProjectionRepository, runEventStore, runTerminalEventPublisher, cancellationRegistry,
                auditRepository, agentRuntime, clock, runTimeout, "openai-compatible:gpt-4.1-mini");
    }

    public DefaultRunDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            Duration runTimeout,
            String modelRef) {
        this(runQueue, runProjectionRepository, runEventStore, runTerminalEventPublisher, cancellationRegistry,
                auditRepository, agentRuntime, clock, runTimeout, defaultAgentDefinition(runTimeout, modelRef), new RuntimeLimits(runTimeout, 64, 64));
    }

    public DefaultRunDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            Duration runTimeout,
            String modelRef,
            ConversationContextAssembler conversationContextAssembler,
            ConversationContextPolicy conversationContextPolicy) {
        this(runQueue, runProjectionRepository, runEventStore, runTerminalEventPublisher, cancellationRegistry,
                auditRepository, agentRuntime, clock, runTimeout, defaultAgentDefinition(runTimeout, modelRef),
                new RuntimeLimits(runTimeout, 64, 64), conversationContextAssembler, conversationContextPolicy);
    }

    public DefaultRunDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            Duration runTimeout,
            AgentDefinition agentDefinition,
            RuntimeLimits runtimeLimits) {
        this(runQueue, runProjectionRepository, runEventStore, runTerminalEventPublisher, cancellationRegistry,
                auditRepository, agentRuntime, clock, runTimeout, agentDefinition, runtimeLimits,
                emptyConversationContextAssembler(), ConversationContextPolicy.defaults());
    }

    public DefaultRunDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            Duration runTimeout,
            AgentDefinition agentDefinition,
            RuntimeLimits runtimeLimits,
            ConversationContextAssembler conversationContextAssembler,
            ConversationContextPolicy conversationContextPolicy) {
        this.runQueue = Objects.requireNonNull(runQueue, "runQueue must not be null");
        this.runProjectionRepository = Objects.requireNonNull(runProjectionRepository, "runProjectionRepository must not be null");
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
        this.runTerminalEventPublisher = Objects.requireNonNull(runTerminalEventPublisher, "runTerminalEventPublisher must not be null");
        this.cancellationRegistry = Objects.requireNonNull(cancellationRegistry, "cancellationRegistry must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.agentRuntime = Objects.requireNonNull(agentRuntime, "agentRuntime must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.runTimeout = Objects.requireNonNull(runTimeout, "runTimeout must not be null");
        this.agentDefinition = Objects.requireNonNull(agentDefinition, "agentDefinition must not be null");
        this.runtimeLimits = Objects.requireNonNull(runtimeLimits, "runtimeLimits must not be null");
        this.conversationContextAssembler = Objects.requireNonNull(conversationContextAssembler, "conversationContextAssembler must not be null");
        Objects.requireNonNull(conversationContextPolicy, "conversationContextPolicy must not be null");
    }

    @Override
    public void dispatch(String workerId) {
        runQueue.claimNext(workerId, clock.instant()).ifPresent(queuedRun -> dispatchClaimed(workerId, queuedRun));
    }

    @Override
    public void dispatchRun(String workerId, String runId) {
        QueuedRun claimed = runQueue.claimNext(workerId, clock.instant()).orElse(null);
        if (claimed == null) {
            return;
        }
        if (!claimed.runId().equals(runId)) {
            throw new IllegalStateException("Claimed run " + claimed.runId() + " did not match requested run " + runId);
        }
        dispatchClaimed(workerId, claimed);
    }

    void dispatchClaimed(String workerId, QueuedRun queuedRun) {
        Instant startedAt = clock.instant();
        String runId = queuedRun.runId();
        CancellationToken token = cancellationRegistry.tokenFor(runId);
        RequestContext requestContext = requestContext(queuedRun);
        try {
            if (!runQueue.markRunning(runId, startedAt)) {
                return;
            }
            String modelRef = effectiveModelRef(queuedRun);
            validateModelRef(modelRef);
            AgentDefinition runAgentDefinition = agentDefinitionFor(modelRef);
            ConversationContextAssembler.Result contextResult = conversationContextAssembler.assemble(requestContext, queuedRun.sessionId(), runId);
            runProjectionRepository.markRunning(runId, startedAt);
            auditRepository.record(requestContext, "run.worker.started", "run", runId, queuedRun.sessionId(), runId, workerStartedDetails(workerId, contextResult.metadata()));
            RunContext context = new RunContext(runAgentDefinition, runInput(queuedRun), sessionContext(queuedRun, contextResult.messages()), workspaceScope(queuedRun), runtimeLimits, token, queuedRun.traceId(), startedAt);
            RunHandle handle = runRuntimeWithTimeout(context, runId);
            Instant finishedAt = clock.instant();
            if (token.isCancellationRequested()) {
                markCancelled(queuedRun, token, requestContext, finishedAt);
            } else {
                markFromHandle(queuedRun, requestContext, handle, finishedAt);
            }
        } catch (TimeoutException ex) {
            Instant finishedAt = clock.instant();
            agentRuntime.cancel(runId, "timeout");
            publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishTimedOutIfAbsent(queuedRun, "timeout", finishedAt));
            runProjectionRepository.markTerminalIfNotTerminal(runId, "TIMED_OUT", Map.of(), Map.of("reason", "timeout"), finishedAt);
            runQueue.markTerminal(runId, "TIMED_OUT", finishedAt);
            auditRepository.record(requestContext, "run.worker.timed_out", "run", runId, queuedRun.sessionId(), runId, Map.of("reason", "timeout"));
        } catch (RuntimeException ex) {
            Instant finishedAt = clock.instant();
            publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishFailedIfAbsent(queuedRun, ex.getClass().getSimpleName(), message(ex), finishedAt));
            runProjectionRepository.markTerminalIfNotTerminal(runId, "FAILED", Map.of(), Map.of("errorType", ex.getClass().getSimpleName(), "message", message(ex)), finishedAt);
            runQueue.markTerminal(runId, "FAILED", finishedAt);
            auditRepository.record(requestContext, "run.worker.failed", "run", runId, queuedRun.sessionId(), runId, Map.of("errorType", ex.getClass().getSimpleName(), "message", message(ex)));
        } finally {
            cancellationRegistry.remove(runId);
        }
    }

    private RunHandle runRuntimeWithTimeout(RunContext context, String runId) throws TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<RunHandle> future = executor.submit(() -> agentRuntime.start(context));
        try {
            return future.get(runTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while dispatching run " + runId, ex);
        } catch (java.util.concurrent.ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(ex.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private void markCompleted(QueuedRun queuedRun, RequestContext requestContext, Instant finishedAt) {
        boolean published = publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishCompletedIfAbsent(queuedRun, finishedAt));
        runProjectionRepository.markTerminalIfNotTerminal(queuedRun.runId(), "COMPLETED", Map.of(), Map.of(), finishedAt);
        runQueue.markTerminal(queuedRun.runId(), "COMPLETED", finishedAt);
        auditRepository.record(requestContext, "run.worker.completed", "run", queuedRun.runId(), queuedRun.sessionId(), queuedRun.runId(), Map.of("publishedTerminalEvent", published));
    }

    private void markFromHandle(QueuedRun queuedRun, RequestContext requestContext, RunHandle handle, Instant finishedAt) {
        if (handle.status() == RunStatus.SUCCEEDED) {
            markCompleted(queuedRun, requestContext, finishedAt);
            return;
        }
        if (handle.status() == RunStatus.CANCELLED) {
            markCancelled(queuedRun, cancellationRegistry.tokenFor(queuedRun.runId()), requestContext, finishedAt);
            return;
        }
        if (handle.status() == RunStatus.POLICY_BLOCKED) {
            publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishFailedIfAbsent(queuedRun, "POLICY_BLOCKED", "policy blocked", finishedAt));
            runProjectionRepository.markTerminalIfNotTerminal(queuedRun.runId(), "POLICY_BLOCKED", Map.of(), Map.of("reason", "policy blocked"), finishedAt);
            runQueue.markTerminal(queuedRun.runId(), "POLICY_BLOCKED", finishedAt);
            auditRepository.record(requestContext, "run.worker.policy_blocked", "run", queuedRun.runId(), queuedRun.sessionId(), queuedRun.runId(), Map.of());
            return;
        }
        publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishFailedIfAbsent(queuedRun, handle.status().name(),
                handle.failureSummary().map(io.github.pi_java.agent.domain.error.FailureSummary::message).orElse(handle.status().name()), finishedAt));
        runProjectionRepository.markTerminalIfNotTerminal(queuedRun.runId(), "FAILED", Map.of(), Map.of("status", handle.status().name()), finishedAt);
        runQueue.markTerminal(queuedRun.runId(), "FAILED", finishedAt);
        auditRepository.record(requestContext, "run.worker.failed", "run", queuedRun.runId(), queuedRun.sessionId(), queuedRun.runId(), Map.of("status", handle.status().name()));
    }

    private void markCancelled(QueuedRun queuedRun, CancellationToken token, RequestContext requestContext, Instant finishedAt) {
        String reason = token.reason().orElse("cancelled");
        boolean hadTerminalEvent = runEventStore.hasTerminalEvent(queuedRun.runId());
        publishTerminalIfNeeded(queuedRun, () -> runTerminalEventPublisher.publishCancelledIfAbsent(queuedRun, reason, finishedAt));
        runProjectionRepository.markTerminalIfNotTerminal(queuedRun.runId(), "CANCELLED", Map.of(), Map.of("reason", reason), finishedAt);
        runQueue.markTerminal(queuedRun.runId(), "CANCELLED", finishedAt);
        if (!hadTerminalEvent) {
            auditRepository.record(requestContext, "run.worker.cancelled", "run", queuedRun.runId(), queuedRun.sessionId(), queuedRun.runId(), Map.of("reason", reason));
        }
    }

    private boolean publishTerminalIfNeeded(QueuedRun queuedRun, TerminalPublisher publisher) {
        if (runEventStore.hasTerminalEvent(queuedRun.runId())) {
            return false;
        }
        return publisher.publish();
    }

    private static RunInput runInput(QueuedRun run) {
        Object text = run.input().get("text");
        return switch (run.inputType().toLowerCase()) {
            case "chat" -> new RunInput.ChatInput(text == null || text.toString().isBlank() ? "chat" : text.toString());
            case "task" -> new RunInput.TaskInput(String.valueOf(run.input().getOrDefault("objective", text == null ? "task" : text)));
            case "tool" -> new RunInput.ToolDrivenInput(String.valueOf(run.input().getOrDefault("toolName", "tool")), run.input());
            case "workflow" -> new RunInput.WorkflowPlannerInput(String.valueOf(run.input().getOrDefault("planRequest", text == null ? "plan" : text)));
            default -> new RunInput.StructuredFormInput(run.input());
        };
    }

    private static SessionContext sessionContext(QueuedRun run, List<SessionEntryPayload.MessageEntry> messages) {
        return new SessionContext(messages, List.of(), List.of(), List.of(), List.of(), java.util.Optional.of(workspaceScope(run)), List.of());
    }

    private static Map<String, Object> workerStartedDetails(String workerId, ConversationContextMetadata metadata) {
        return Map.of(
                "workerId", workerId,
                "contextIncludedCount", metadata.includedCount(),
                "contextDroppedCount", metadata.droppedCount(),
                "contextExcludedCount", metadata.excludedCount(),
                "contextMaxChars", metadata.maxTotalCharacters(),
                "contextResultChars", metadata.resultingCharacters(),
                "contextTruncated", metadata.truncated());
    }

    private static ConversationContextAssembler emptyConversationContextAssembler() {
        return new ConversationContextAssembler(new ConversationQueryService() {
            @Override
            public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
                return new PageResponse<>(List.of(), limit, null, null, false);
            }

            @Override
            public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
                return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
            }
        }, ConversationContextPolicy.defaults());
    }

    private static WorkspaceScope workspaceScope(QueuedRun run) {
        return new WorkspaceScope(run.tenantId(), run.userId(), run.sessionId(), run.runId(), run.workspaceId(), Set.of(), Set.of());
    }

    private static RequestContext requestContext(QueuedRun run) {
        return new RequestContext(new SecurityPrincipalContext(run.tenantId(), run.userId(), Set.of()), new CorrelationContext(run.traceId(), run.correlationId(), run.runId()));
    }

    private String effectiveModelRef(QueuedRun run) {
        RunProviderMetadata metadata = run.providerMetadata();
        if (metadata != null) {
            String selected = text(metadata.selectedModelRef());
            if (selected != null) {
                return selected;
            }
            String requested = text(metadata.requestedModelRef());
            if (requested != null) {
                return requested;
            }
            String providerId = text(metadata.resolvedProviderId());
            String modelId = text(metadata.resolvedModelId());
            if (providerId != null && modelId != null) {
                return providerId + ":" + modelId;
            }
        }
        return agentDefinition.modelRef();
    }

    private AgentDefinition agentDefinitionFor(String modelRef) {
        if (agentDefinition.modelRef().equals(modelRef)) {
            return agentDefinition;
        }
        return new AgentDefinition(agentDefinition.agentId(), agentDefinition.displayName(), agentDefinition.instructions(), modelRef,
                agentDefinition.allowedToolScopes(), agentDefinition.policyRefs(), agentDefinition.runtimeLimits(),
                agentDefinition.supportedInputModes(), agentDefinition.workspacePolicyRef(), agentDefinition.outputPolicyRef());
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static AgentDefinition defaultAgentDefinition(Duration runTimeout, String modelRef) {
        RuntimeLimits limits = new RuntimeLimits(runTimeout, 64, 64);
        return new AgentDefinition(new AgentId("default-agent"), "Default Agent", "Execute queued run", modelRef, Set.of(), Set.of("default"), limits, Set.of(InteractionMode.CHAT, InteractionMode.TASK), "default-workspace-policy", "default-output-policy");
    }

    private static String validateModelRef(String modelRef) {
        return ProviderModelRef.parse(modelRef).canonical();
    }

    private static String message(Throwable ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    @FunctionalInterface
    private interface TerminalPublisher {
        boolean publish();
    }
}
