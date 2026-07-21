package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@TestConfiguration(proxyBeanMethods = false)
public class InMemoryCloudE2EConfiguration {

    @Bean
    InMemoryStores inMemoryStores() {
        return new InMemoryStores();
    }

    @Bean
    @Primary
    SessionRepository sessionRepository(InMemoryStores stores) {
        return new SessionRepo(stores);
    }

    @Bean
    @Primary
    RunProjectionRepository runProjectionRepository(InMemoryStores stores) {
        return new RunProjectionRepo(stores);
    }

    @Bean
    @Primary
    RunEventStore runEventStore(InMemoryStores stores) {
        return new EventStore(stores);
    }

    @Bean
    @Primary
    RunQueue runQueue(InMemoryStores stores) {
        return new QueueStore(stores);
    }

    @Bean
    @Primary
    AuditRepository auditRepository(InMemoryStores stores) {
        return stores::recordAudit;
    }

    @Bean
    @Primary
    TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new NoopTransactionManager());
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }

    static final class InMemoryStores {
        private final Map<String, SessionResponse> sessions = new ConcurrentHashMap<>();
        private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();
        private final Map<String, List<RunEvent>> events = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<AuditRecord> audits = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<QueuedRun> queue = new ConcurrentLinkedQueue<>();

        public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) {
            SessionResponse response = new SessionResponse(context.tenantId(), context.userId(), sessionId, request.workspaceId(),
                    "entry-" + sessionId, "ACTIVE", now, now, request.metadata());
            sessions.put(sessionId, response);
            return response;
        }

        public Optional<SessionResponse> findById(RequestContext context, String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        public SessionHistoryResponse history(RequestContext context, String sessionId) {
            return new SessionHistoryResponse(sessions.get(sessionId), List.of());
        }

        public PageResponse<SessionSummaryDto> listRecent(RequestContext context, int limit, String cursor) {
            int pageSize = limit > 0 ? limit : 20;
            List<SessionSummaryDto> rows = sessions.values().stream()
                    .filter(session -> context.tenantId().equals(session.tenantId()))
                    .filter(session -> context.userId().equals(session.userId()))
                    .map(session -> toSessionSummary(context, session))
                    .sorted(Comparator.comparing(SessionSummaryDto::lastActivityAt).reversed()
                            .thenComparing(SessionSummaryDto::sessionId, Comparator.reverseOrder()))
                    .limit(pageSize + 1L)
                    .toList();
            boolean hasMore = rows.size() > pageSize;
            List<SessionSummaryDto> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
            return new PageResponse<>(page, pageSize, null, null, hasMore);
        }

        private SessionSummaryDto toSessionSummary(RequestContext context, SessionResponse session) {
            List<RunRecord> sessionRuns = runsForSession(context, session.sessionId());
            RunRecord first = sessionRuns.isEmpty() ? null : sessionRuns.get(0);
            RunRecord latest = sessionRuns.isEmpty() ? null : sessionRuns.get(sessionRuns.size() - 1);
            String title = firstUserText(first == null ? Map.of() : first.request().input());
            String preview = latestVisibleText(latest);
            Instant lastActivityAt = latest == null ? session.updatedAt() : latest.response().updatedAt();
            return new SessionSummaryDto(
                    session.sessionId(),
                    title == null || title.isBlank() ? "New conversation" : title,
                    session.status(),
                    preview == null ? "" : preview,
                    lastActivityAt,
                    session.createdAt(),
                    latest == null ? null : latest.response().runId(),
                    latest == null ? null : latest.response().status(),
                    session.metadata());
        }

        private List<RunRecord> runsForSession(RequestContext context, String sessionId) {
            return runs.values().stream()
                    .filter(record -> sessionId.equals(record.response().sessionId()))
                    .filter(record -> context.tenantId().equals(record.response().tenantId()))
                    .filter(record -> context.userId().equals(record.response().userId()))
                    .sorted(Comparator.comparing((RunRecord record) -> record.response().createdAt())
                            .thenComparing(record -> record.response().runId()))
                    .toList();
        }

        private String latestVisibleText(RunRecord latest) {
            if (latest == null) {
                return null;
            }
            StringBuilder assistantText = new StringBuilder();
            for (RunEvent event : events.getOrDefault(latest.response().runId(), List.of()).stream()
                    .sorted(Comparator.comparingLong(RunEvent::sequence))
                    .toList()) {
                if (event.payload() instanceof RunEventPayload.ModelDeltaPayload delta
                        && delta.textDelta() != null
                        && !delta.textDelta().isBlank()) {
                    assistantText.append(delta.textDelta());
                }
            }
            if (!assistantText.isEmpty()) {
                return assistantText.toString();
            }
            return firstUserText(latest.request().input());
        }

        private static String firstUserText(Map<String, Object> input) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            Object value = input.get("text");
            if (value == null) {
                value = input.get("prompt");
            }
            return value == null ? null : value.toString();
        }

        public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) {
            Instant now = Instant.now();
            RunProviderMetadata providerMetadata = safeProviderMetadata(request.metadata());
            runs.put(runId, new RunRecord(new RunResponse(context.tenantId(), context.userId(), sessionId, runId,
                    request.workspaceId(), "QUEUED", context.traceId(), context.correlationId(), now, now, providerMetadata), request, Map.of(), Map.of()));
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

        public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) {
            return Optional.ofNullable(runs.get(runId)).map(RunRecord::response);
        }

        public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) {
            RunResponse run = runs.get(runId).response();
            return new RunStatusResponse(sessionId, runId, run.status(), isTerminal(run.status()), run.updatedAt(), run.traceId(), run.correlationId());
        }

        public boolean markRunning(String runId, Instant startedAt) {
            updateStatus(runId, "RUNNING", startedAt, Map.of(), Map.of());
            return true;
        }

        public boolean requestCancellation(String runId, String reason, Instant requestedAt) {
            updateStatus(runId, "CANCELLING", requestedAt, Map.of(), Map.of("reason", reason));
            return true;
        }

        public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) {
            if (isTerminal(runs.get(runId).response().status())) {
                return false;
            }
            updateStatus(runId, status, finishedAt, terminalResult, failure);
            return true;
        }

        public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
            return new RunDetailResponse(runs.get(runId).response(), List.of(), List.of(), List.of(), List.of(), getRunResult(context, sessionId, runId));
        }

        public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
            RunRecord record = runs.get(runId);
            return new RunResultResponse(runId, record.response().status(), record.result(), record.failure());
        }

        public PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor) {
            int pageSize = limit > 0 ? limit : 20;
            List<ConversationRunView> rows = runsForSession(context, sessionId).stream()
                    .limit(pageSize + 1L)
                    .map(record -> new ConversationRunView(record.response().runId(), record.response().createdAt(),
                            record.request().input(), record.response().status(), record.response().providerMetadata()))
                    .toList();
            boolean hasMore = rows.size() > pageSize;
            List<ConversationRunView> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
            return new PageResponse<>(page, pageSize, null, null, hasMore);
        }

        public void append(RunEvent event) {
            events.computeIfAbsent(event.runId().value(), ignored -> new CopyOnWriteArrayList<>()).add(event);
        }

        public List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
            return events.getOrDefault(runId, List.of()).stream()
                    .sorted(Comparator.comparingLong(RunEvent::sequence))
                    .filter(event -> event.sequence() > afterSequence)
                    .limit(limit)
                    .toList();
        }

        public List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
            return events.getOrDefault(runId, List.of()).stream()
                    .filter(event -> sessionId.equals(event.sessionId().value()))
                    .filter(event -> context.tenantId().equals(event.tenantId().value()))
                    .filter(event -> context.userId().equals(event.userId().value()))
                    .sorted(Comparator.comparingLong(RunEvent::sequence))
                    .filter(event -> event.sequence() > afterSequence)
                    .limit(limit)
                    .toList();
        }

        public Optional<RunEvent> findLastByRun(String runId) {
            return events.getOrDefault(runId, List.of()).stream().max(Comparator.comparingLong(RunEvent::sequence));
        }

        public boolean hasTerminalEvent(String runId) {
            return events.getOrDefault(runId, List.of()).stream().anyMatch(event -> isTerminalType(event.type()));
        }

        public void enqueue(QueuedRun run) {
            queue.add(run);
        }

        public Optional<QueuedRun> claimNext(String workerId, Instant now) {
            return Optional.ofNullable(queue.poll());
        }

        public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) {
            updateStatus(runId, terminalStatus, finishedAt, Map.of(), Map.of());
            return true;
        }

        public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) {
            return Optional.empty();
        }

        public boolean removeIfTerminal(String runId) {
            return isTerminal(runs.get(runId).response().status());
        }

        public void recordAudit(RequestContext context, String action, String resourceType, String resourceId,
                                String sessionId, String runId, Map<String, Object> details) {
            audits.add(new AuditRecord(context.tenantId(), context.userId(), action, resourceType, resourceId,
                    sessionId, runId, Map.copyOf(details)));
        }

        public List<AuditRecord> auditsForRun(String runId) {
            return audits.stream().filter(audit -> runId.equals(audit.runId())).toList();
        }

        private void updateStatus(String runId, String status, Instant updatedAt, Map<String, Object> result, Map<String, Object> failure) {
            Map<String, Object> safeResult = result == null ? Map.of() : result;
            Map<String, Object> safeFailure = failure == null ? Map.of() : failure;
            runs.computeIfPresent(runId, (id, existing) -> {
                Map<String, Object> nextResult = safeResult.isEmpty() ? existing.result() : new LinkedHashMap<>(safeResult);
                Map<String, Object> nextFailure = safeFailure.isEmpty() ? existing.failure() : new LinkedHashMap<>(safeFailure);
                return new RunRecord(new RunResponse(existing.response().tenantId(), existing.response().userId(),
                    existing.response().sessionId(), existing.response().runId(), existing.response().workspaceId(), status,
                    existing.response().traceId(), existing.response().correlationId(), existing.response().createdAt(), updatedAt,
                    existing.response().providerMetadata()),
                    existing.request(), nextResult, nextFailure);
            });
        }

        private static boolean isTerminal(String status) {
            return List.of("COMPLETED", "FAILED", "CANCELLED", "TIMED_OUT", "POLICY_BLOCKED").contains(status);
        }

        private static boolean isTerminalType(RunEventType type) {
            return List.of(RunEventType.RUN_COMPLETED, RunEventType.RUN_FAILED, RunEventType.RUN_CANCELLED, RunEventType.RUN_POLICY_BLOCKED).contains(type);
        }

        private record RunRecord(RunResponse response, CreateRunRequest request, Map<String, Object> result, Map<String, Object> failure) {
        }

        public record AuditRecord(String tenantId, String userId, String action, String resourceType, String resourceId,
                                  String sessionId, String runId, Map<String, Object> details) {
        }
    }

    private record SessionRepo(InMemoryStores stores) implements SessionRepository {
        public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) { return stores.create(context, request, sessionId, now); }
        public Optional<SessionResponse> findById(RequestContext context, String sessionId) { return stores.findById(context, sessionId); }
        public SessionHistoryResponse history(RequestContext context, String sessionId) { return stores.history(context, sessionId); }
        public PageResponse<SessionSummaryDto> listRecent(RequestContext context, int limit, String cursor) { return stores.listRecent(context, limit, cursor); }
    }

    private record RunProjectionRepo(InMemoryStores stores) implements RunProjectionRepository {
        public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { stores.createRun(context, sessionId, runId, request); }
        public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return stores.findRun(context, sessionId, runId); }
        public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return stores.getStatus(context, sessionId, runId); }
        public boolean markRunning(String runId, Instant startedAt) { return stores.markRunning(runId, startedAt); }
        public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return stores.requestCancellation(runId, reason, requestedAt); }
        public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { return stores.markTerminalIfNotTerminal(runId, status, terminalResult, failure, finishedAt); }
        public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return stores.getRunDetail(context, sessionId, runId); }
        public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return stores.listSteps(context, sessionId, runId, limit); }
        public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return stores.listMessages(context, sessionId, runId, limit); }
        public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return stores.listToolCalls(context, sessionId, runId, limit); }
        public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return stores.getRunResult(context, sessionId, runId); }
        public PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor) { return stores.listRunsBySession(context, sessionId, limit, cursor); }
    }

    private record EventStore(InMemoryStores stores) implements RunEventStore {
        public void append(RunEvent event) { stores.append(event); }
        public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return stores.listByRun(runId, afterSequence, limit); }
        public List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit) { return stores.listBySessionRun(context, sessionId, runId, afterSequence, limit); }
        public Optional<RunEvent> findLastByRun(String runId) { return stores.findLastByRun(runId); }
        public boolean hasTerminalEvent(String runId) { return stores.hasTerminalEvent(runId); }
    }

    private record QueueStore(InMemoryStores stores) implements RunQueue {
        public void enqueue(QueuedRun run) { stores.enqueue(run); }
        public Optional<QueuedRun> claimNext(String workerId, Instant now) { return stores.claimNext(workerId, now); }
        public boolean markRunning(String runId, Instant startedAt) { return stores.markRunning(runId, startedAt); }
        public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { return stores.markTerminal(runId, terminalStatus, finishedAt); }
        public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return stores.cancelQueuedAndReturn(runId, reason, cancelledAt); }
        public boolean removeIfTerminal(String runId) { return stores.removeIfTerminal(runId); }
    }
}
