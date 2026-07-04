package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultRunCommandService implements RunCommandService {

    private final RunProjectionRepository runProjectionRepository;
    private final RunQueue runQueue;
    private final CancellationRegistry cancellationRegistry;
    private final RunTerminalEventPublisher runTerminalEventPublisher;
    private final AuditRepository auditRepository;
    private final Supplier<String> runIdSupplier;
    private final Clock clock;

    public DefaultRunCommandService(
            RunProjectionRepository runProjectionRepository,
            RunQueue runQueue,
            CancellationRegistry cancellationRegistry,
            RunTerminalEventPublisher runTerminalEventPublisher,
            AuditRepository auditRepository,
            Supplier<String> runIdSupplier,
            Clock clock) {
        this.runProjectionRepository = Objects.requireNonNull(runProjectionRepository, "runProjectionRepository must not be null");
        this.runQueue = Objects.requireNonNull(runQueue, "runQueue must not be null");
        this.cancellationRegistry = Objects.requireNonNull(cancellationRegistry, "cancellationRegistry must not be null");
        this.runTerminalEventPublisher = Objects.requireNonNull(runTerminalEventPublisher, "runTerminalEventPublisher must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public RunResponse createRun(RequestContext context, String sessionId, CreateRunRequest request) {
        String runId = runIdSupplier.get();
        runProjectionRepository.createRun(context, sessionId, runId, request);
        Instant now = clock.instant();
        RunProviderMetadata providerMetadata = safeProviderMetadata(request.metadata());
        runQueue.enqueue(new QueuedRun(runId, sessionId, context.tenantId(), context.userId(), request.workspaceId(),
                context.traceId(), context.correlationId(), request.inputType(), request.input(), providerMetadata, now, 0));
        auditRepository.record(context, "run.create", "run", runId, sessionId, runId,
                Map.of("workspaceId", request.workspaceId(), "inputType", request.inputType()));
        return runProjectionRepository.findRun(context, sessionId, runId)
                .orElseThrow(() -> new NoSuchElementException("run not found after create: " + runId));
    }

    @Override
    public RunStatusResponse cancelRun(RequestContext context, String sessionId, String runId, CancelRunRequest request) {
        RunStatusResponse current = runProjectionRepository.getStatus(context, sessionId, runId);
        String reason = normalizeReason(request.reason());
        if (current.terminal()) {
            auditRepository.record(context, "run.cancel.noop_terminal", "run", runId, sessionId, runId,
                    Map.of("reason", reason, "status", current.status()));
            return current;
        }

        Instant now = clock.instant();
        runProjectionRepository.requestCancellation(runId, reason, now);
        auditRepository.record(context, "run.cancel.requested", "run", runId, sessionId, runId,
                Map.of("reason", reason));

        var cancelledQueuedRun = runQueue.cancelQueuedAndReturn(runId, reason, now);
        if (cancelledQueuedRun.isPresent()) {
            QueuedRun run = cancelledQueuedRun.get();
            runProjectionRepository.markTerminalIfNotTerminal(runId, "CANCELLED", Map.of(), Map.of("reason", reason), now);
            runQueue.markTerminal(runId, "CANCELLED", now);
            runTerminalEventPublisher.publishCancelledIfAbsent(run, reason, now);
            return runProjectionRepository.getStatus(context, sessionId, runId);
        }

        cancellationRegistry.requestCancellation(runId, reason);
        return runProjectionRepository.getStatus(context, sessionId, runId);
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "cancelled" : reason;
    }

    private static RunProviderMetadata safeProviderMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return RunProviderMetadata.EMPTY;
        }
        return new RunProviderMetadata(
                string(metadata.get("requestedModelRef")),
                string(metadata.get("selectedModelRef")),
                firstString(metadata, "resolvedProviderId", "providerId", "provider_id"),
                firstString(metadata, "resolvedModelId", "modelId", "model_id"),
                string(metadata.get("fallbackMode")),
                string(metadata.get("readinessState")),
                redactSummary(string(metadata.get("safeErrorSummary"))));
    }

    private static String firstString(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            String value = string(metadata.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private static String redactSummary(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("(?i)(api[_-]?key\\s*[=:]\\s*)[^\\s,;]+", "$1[REDACTED]")
                .replaceAll("sk-[A-Za-z0-9._-]+", "[REDACTED]");
    }
}
