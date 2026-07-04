package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.DefaultRunCommandService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RunProviderModelResolutionFlowTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Test
    void consoleCreateRunRequestIncludesSelectedProviderModelSnapshot(@TempDir Path tmp) {
        ProviderConfigStore store = new ProviderConfigStore(tmp.resolve("provider.db").toString());
        store.update(new ProviderConfig(true, "https://example.test/v1", "sk-test", "gpt-4o-mini", "openai-compatible", "/chat/completions"));
        CapturingBridge bridge = new CapturingBridge();
        ConsoleView view = new ConsoleView(new io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient(), new EventStreamClient(),
                new DefaultAgentCatalogQueryService(), bridge, new io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer(),
                store, null);

        view.planChatSubmission("hello model");

        assertThat(bridge.createdRunRequest.metadata())
                .containsEntry("requestedModelRef", "openai-compatible:gpt-4o-mini")
                .containsEntry("selectedModelRef", "openai-compatible:gpt-4o-mini")
                .containsEntry("providerId", "openai-compatible")
                .containsEntry("modelId", "gpt-4o-mini")
                .containsEntry("readinessState", "READY");
        assertThat(bridge.createdRunRequest.metadata()).doesNotContainKeys("apiKey", "authorization", "headers", "providerConfig");
    }

    @Test
    void commandServiceNormalizesMetadataAndEnqueuesQueuedRunSnapshot() {
        CapturingProjectionRepository projections = new CapturingProjectionRepository();
        CapturingQueue queue = new CapturingQueue();
        DefaultRunCommandService service = new DefaultRunCommandService(projections, queue,
                new NoopCancellationRegistry(), new NoopTerminalPublisher(), new NoopAuditRepository(),
                () -> "run-1", Clock.fixed(NOW, ZoneOffset.UTC));

        service.createRun(context(), "session-1", new CreateRunRequest("agent", "chat", Map.of("text", "hello"), "workspace-1", Map.of(
                "selectedModelRef", "openai-compatible:gpt-4o-mini",
                "providerId", "openai-compatible",
                "modelId", "gpt-4o-mini",
                "readinessState", "READY",
                "apiKey", "sk-secret")));

        assertThat(queue.enqueued.providerMetadata()).isEqualTo(new RunProviderMetadata(
                null, "openai-compatible:gpt-4o-mini", "openai-compatible", "gpt-4o-mini", null, "READY", null));
        assertThat(projections.createdRequest.metadata()).containsEntry("apiKey", "sk-secret");
    }

    @Test
    void queuedSnapshotDoesNotChangeWhenSelectorChangesAfterRunCreation(@TempDir Path tmp) {
        ProviderConfigStore store = new ProviderConfigStore(tmp.resolve("provider.db").toString());
        store.update(new ProviderConfig(true, "https://example.test/v1", "sk-test", "first-model", "openai-compatible", "/chat/completions"));
        CapturingBridge bridge = new CapturingBridge();
        ConsoleView view = new ConsoleView(new io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient(), new EventStreamClient(),
                new DefaultAgentCatalogQueryService(), bridge, new io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer(),
                store, null);

        view.planChatSubmission("first request");
        Map<String, Object> firstSnapshot = bridge.createdRunRequest.metadata();
        store.update(new ProviderConfig(true, "https://example.test/v1", "sk-test", "second-model", "openai-compatible", "/chat/completions"));

        assertThat(firstSnapshot).containsEntry("selectedModelRef", "openai-compatible:first-model");
        assertThat(firstSnapshot).doesNotContainValue("second-model");
    }

    private static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static final class CapturingBridge implements ConsoleRunExecutionBridge {
        CreateRunRequest createdRunRequest;

        @Override public SessionResponse createSession() {
            return new SessionResponse("tenant-1", "user-1", "session-1", "workspace-1", null, "ACTIVE", NOW, NOW, Map.of());
        }

        @Override public RunResponse createRun(String sessionId, CreateRunRequest request) {
            createdRunRequest = request;
            return new RunResponse("tenant-1", "user-1", sessionId, "run-1", request.workspaceId(), "QUEUED", "trace-1", "correlation-1", NOW, NOW);
        }

        @Override public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            return new EventHistoryResponse(sessionId, runId, List.of(), afterSequence, afterSequence, false);
        }

        @Override public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) { throw new UnsupportedOperationException(); }
        @Override public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) { return new PageResponse<>(List.of(), limit, null, null, false); }
        @Override public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) { return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of()); }
    }

    private static final class CapturingProjectionRepository implements RunProjectionRepository {
        CreateRunRequest createdRequest;
        @Override public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { createdRequest = request; }
        @Override public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return Optional.of(new RunResponse(context.tenantId(), context.userId(), sessionId, runId, "workspace-1", "QUEUED", context.traceId(), context.correlationId(), NOW, NOW)); }
        @Override public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return null; }
        @Override public boolean markRunning(String runId, Instant startedAt) { return false; }
        @Override public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return false; }
        @Override public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { return false; }
        @Override public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return null; }
        @Override public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return null; }
        @Override public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return null; }
    }

    private static final class CapturingQueue implements RunQueue {
        QueuedRun enqueued;
        @Override public void enqueue(QueuedRun run) { enqueued = run; }
        @Override public Optional<QueuedRun> claimNext(String workerId, Instant now) { return Optional.empty(); }
        @Override public boolean markRunning(String runId, Instant startedAt) { return false; }
        @Override public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { return false; }
        @Override public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return Optional.empty(); }
        @Override public boolean removeIfTerminal(String runId) { return false; }
    }

    private static final class NoopCancellationRegistry implements CancellationRegistry {
        @Override public io.github.pi_java.agent.domain.runtime.CancellationToken tokenFor(String runId) { return new io.github.pi_java.agent.domain.runtime.CancellationToken(); }
        @Override public Optional<io.github.pi_java.agent.domain.runtime.CancellationToken> activeToken(String runId) { return Optional.empty(); }
        @Override public boolean requestCancellation(String runId, String reason) { return false; }
        @Override public void remove(String runId) { }
    }

    private static final class NoopTerminalPublisher implements RunTerminalEventPublisher {
        @Override public boolean publishCompletedIfAbsent(QueuedRun run, Instant finishedAt) { return false; }
        @Override public boolean publishCancelledIfAbsent(QueuedRun run, String reason, Instant finishedAt) { return false; }
        @Override public boolean publishFailedIfAbsent(QueuedRun run, String errorType, String message, Instant finishedAt) { return false; }
        @Override public boolean publishTimedOutIfAbsent(QueuedRun run, String reason, Instant finishedAt) { return false; }
    }

    private static final class NoopAuditRepository implements AuditRepository {
        @Override public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) { }
    }
}
