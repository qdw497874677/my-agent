package io.github.pi_java.agent.adapter.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.pi_java.agent.adapter.web.provider.SqliteLocalPersistence;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

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

@Profile("local")
@Configuration(proxyBeanMethods = false)
public class LocalDevRuntimeBeanConfiguration {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Bean
    LocalDevStores localDevStores(
            @Value("${pi.local.persist:true}") boolean persist,
            @Value("${pi.local.db-path:data/pi-local.db}") String dbPath) {
        SqliteLocalPersistence persistence = persist ? new SqliteLocalPersistence(dbPath) : null;
        LocalDevStores stores = new LocalDevStores(persistence);
        stores.loadAll();
        return stores;
    }

    @Bean
    @Primary
    SessionRepository localSessionRepository(LocalDevStores stores) {
        return new LocalSessionRepository(stores);
    }

    @Bean
    @Primary
    RunProjectionRepository localRunProjectionRepository(LocalDevStores stores) {
        return new LocalRunProjectionRepository(stores);
    }

    @Bean
    @Primary
    RunEventStore localRunEventStore(LocalDevStores stores) {
        return new LocalRunEventStore(stores);
    }

    @Bean
    @Primary
    RunQueue localRunQueue(LocalDevStores stores) {
        return new LocalRunQueue(stores);
    }

    @Bean
    @Primary
    AuditRepository localAuditRepository(LocalDevStores stores) {
        return stores::recordAudit;
    }

    @Bean
    @Primary
    TransactionTemplate localTransactionTemplate() {
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

    static final class LocalDevStores {
        private final Map<String, SessionResponse> sessions = new ConcurrentHashMap<>();
        private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();
        private final Map<String, List<RunEvent>> events = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<QueuedRun> queue = new ConcurrentLinkedQueue<>();
        private final SqliteLocalPersistence persistence;

        LocalDevStores(SqliteLocalPersistence persistence) {
            this.persistence = persistence;
        }

        @SuppressWarnings("unchecked")
        void loadAll() {
            if (persistence == null) return;
            for (Map<String, String> row : persistence.loadSessions()) {
                String sessionId = row.get("session_id");
                try {
                    sessions.put(sessionId, new SessionResponse(
                            row.get("tenant_id"), row.get("user_id"), sessionId,
                            row.get("workspace_id"), row.getOrDefault("current_entry_id", "entry-" + sessionId),
                            row.get("status"),
                            Instant.parse(row.get("created_at")),
                            Instant.parse(row.get("updated_at")),
                            parseJsonMap(row.get("metadata_json"))));
                } catch (Exception ignored) { }
            }
            for (Map<String, String> row : persistence.loadRuns()) {
                String runId = row.get("run_id");
                try {
                    RunResponse resp = new RunResponse(
                            row.get("tenant_id"), row.get("user_id"), row.get("session_id"), runId,
                            row.get("workspace_id"), row.get("status"),
                            row.get("trace_id"), row.get("correlation_id"),
                            Instant.parse(row.get("created_at")), Instant.parse(row.get("updated_at")));
                    runs.put(runId, new RunRecord(resp, parseJsonMap(row.get("result_json")), parseJsonMap(row.get("failure_json"))));
                } catch (Exception ignored) { }
            }
            for (Map<String, String> row : persistence.loadEvents()) {
                String runId = row.get("run_id");
                try {
                    RunEvent event = JSON.readValue(row.get("payload_json"), RunEvent.class);
                    events.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(event);
                } catch (Exception ignored) { }
            }
        }

        SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) {
            SessionResponse response = new SessionResponse(context.tenantId(), context.userId(), sessionId,
                    request.workspaceId(), "entry-" + sessionId, "ACTIVE", now, now, request.metadata());
            sessions.put(sessionId, response);
            if (persistence != null) {
                persistence.saveSession(sessionId, context.tenantId(), context.userId(), request.workspaceId(),
                        "ACTIVE", now.toString(), now.toString(), toJson(request.metadata()));
            }
            return response;
        }

        Optional<SessionResponse> findById(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        SessionHistoryResponse history(String sessionId) {
            return new SessionHistoryResponse(sessions.get(sessionId), List.of());
        }

        void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) {
            Instant now = Instant.now();
            RunResponse response = new RunResponse(context.tenantId(), context.userId(), sessionId, runId,
                    request.workspaceId(), "QUEUED", context.traceId(), context.correlationId(), now, now);
            runs.put(runId, new RunRecord(response, Map.of(), Map.of()));
            if (persistence != null) {
                persistence.saveRun(runId, sessionId, context.tenantId(), context.userId(), request.workspaceId(),
                        "QUEUED", now.toString(), now.toString(), null, null);
            }
        }

        Optional<RunResponse> findRun(String runId) {
            return Optional.ofNullable(runs.get(runId)).map(RunRecord::response);
        }

        RunStatusResponse getStatus(String sessionId, String runId) {
            RunResponse run = runs.get(runId).response();
            return new RunStatusResponse(sessionId, runId, run.status(), isTerminal(run.status()), run.updatedAt(),
                    run.traceId(), run.correlationId());
        }

        boolean markRunning(String runId, Instant startedAt) {
            updateStatus(runId, "RUNNING", startedAt, Map.of(), Map.of());
            return true;
        }

        boolean requestCancellation(String runId, String reason, Instant requestedAt) {
            updateStatus(runId, "CANCELLING", requestedAt, Map.of(), Map.of("reason", reason));
            return true;
        }

        boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult,
                                          Map<String, Object> failure, Instant finishedAt) {
            RunRecord record = runs.get(runId);
            if (record == null || isTerminal(record.response().status())) {
                return false;
            }
            updateStatus(runId, status, finishedAt, terminalResult, failure);
            return true;
        }

        RunDetailResponse getRunDetail(String runId) {
            RunRecord record = runs.get(runId);
            return new RunDetailResponse(record.response(), List.of(), List.of(), List.of(), List.of(), getRunResult(runId));
        }

        RunResultResponse getRunResult(String runId) {
            RunRecord record = runs.get(runId);
            return new RunResultResponse(runId, record.response().status(), record.result(), record.failure());
        }

        void append(RunEvent event) {
            events.computeIfAbsent(event.runId().value(), ignored -> new CopyOnWriteArrayList<>()).add(event);
            if (persistence != null) {
                persistence.appendEvent(event.eventId(), event.runId().value(), event.sequence(),
                        event.type().name(), event.timestamp().toString(), toJson(event));
            }
        }

        List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
            return events.getOrDefault(runId, List.of()).stream()
                    .sorted(Comparator.comparingLong(RunEvent::sequence))
                    .filter(event -> event.sequence() > afterSequence)
                    .limit(limit)
                    .toList();
        }

        Optional<RunEvent> findLastByRun(String runId) {
            return events.getOrDefault(runId, List.of()).stream().max(Comparator.comparingLong(RunEvent::sequence));
        }

        boolean hasTerminalEvent(String runId) {
            return events.getOrDefault(runId, List.of()).stream().anyMatch(event -> isTerminalType(event.type()));
        }

        void enqueue(QueuedRun run) {
            queue.add(run);
        }

        Optional<QueuedRun> claimNext() {
            return Optional.ofNullable(queue.poll());
        }

        Optional<QueuedRun> cancelQueuedAndReturn(String runId) {
            return Optional.empty();
        }

        boolean removeIfTerminal(String runId) {
            RunRecord record = runs.get(runId);
            return record != null && isTerminal(record.response().status());
        }

        void recordAudit(RequestContext context, String action, String resourceType, String resourceId,
                         String sessionId, String runId, Map<String, Object> details) {
        }

        private void updateStatus(String runId, String status, Instant updatedAt, Map<String, Object> result,
                                  Map<String, Object> failure) {
            Map<String, Object> safeResult = result == null ? Map.of() : result;
            Map<String, Object> safeFailure = failure == null ? Map.of() : failure;
            runs.computeIfPresent(runId, (id, existing) -> {
                Map<String, Object> nextResult = safeResult.isEmpty() ? existing.result() : new LinkedHashMap<>(safeResult);
                Map<String, Object> nextFailure = safeFailure.isEmpty() ? existing.failure() : new LinkedHashMap<>(safeFailure);
                RunResponse current = existing.response();
                RunResponse next = new RunResponse(current.tenantId(), current.userId(), current.sessionId(),
                        current.runId(), current.workspaceId(), status, current.traceId(), current.correlationId(),
                        current.createdAt(), updatedAt);
                if (persistence != null) {
                    persistence.saveRun(runId, current.sessionId(), current.tenantId(), current.userId(),
                            current.workspaceId(), status, current.createdAt().toString(), updatedAt.toString(),
                            toJson(nextResult), toJson(nextFailure));
                }
                return new RunRecord(next, nextResult, nextFailure);
            });
        }

        private static boolean isTerminal(String status) {
            return List.of("COMPLETED", "FAILED", "CANCELLED", "TIMED_OUT", "POLICY_BLOCKED").contains(status);
        }

        private static boolean isTerminalType(RunEventType type) {
            return List.of(RunEventType.RUN_COMPLETED, RunEventType.RUN_FAILED, RunEventType.RUN_CANCELLED,
                    RunEventType.RUN_POLICY_BLOCKED).contains(type);
        }

        private static String toJson(Object value) {
            try {
                return JSON.writeValueAsString(value == null ? Map.of() : value);
            } catch (Exception e) {
                return "{}";
            }
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> parseJsonMap(String json) {
            if (json == null || json.isBlank()) return Map.of();
            try {
                return JSON.readValue(json, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        }

        private record RunRecord(RunResponse response, Map<String, Object> result, Map<String, Object> failure) {
        }
    }

    private record LocalSessionRepository(LocalDevStores stores) implements SessionRepository {
        public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) { return stores.create(context, request, sessionId, now); }
        public Optional<SessionResponse> findById(RequestContext context, String sessionId) { return stores.findById(sessionId); }
        public SessionHistoryResponse history(RequestContext context, String sessionId) { return stores.history(sessionId); }
    }

    private record LocalRunProjectionRepository(LocalDevStores stores) implements RunProjectionRepository {
        public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) { stores.createRun(context, sessionId, runId, request); }
        public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) { return stores.findRun(runId); }
        public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) { return stores.getStatus(sessionId, runId); }
        public boolean markRunning(String runId, Instant startedAt) { return stores.markRunning(runId, startedAt); }
        public boolean requestCancellation(String runId, String reason, Instant requestedAt) { return stores.requestCancellation(runId, reason, requestedAt); }
        public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) { return stores.markTerminalIfNotTerminal(runId, status, terminalResult, failure, finishedAt); }
        public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) { return stores.getRunDetail(runId); }
        public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) { return new PageResponse<>(List.of(), limit, null, null, false); }
        public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) { return new PageResponse<>(List.of(), limit, null, null, false); }
        public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) { return new PageResponse<>(List.of(), limit, null, null, false); }
        public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) { return stores.getRunResult(runId); }
    }

    private record LocalRunEventStore(LocalDevStores stores) implements RunEventStore {
        public void append(RunEvent event) { stores.append(event); }
        public List<RunEvent> listByRun(String runId, long afterSequence, int limit) { return stores.listByRun(runId, afterSequence, limit); }
        public Optional<RunEvent> findLastByRun(String runId) { return stores.findLastByRun(runId); }
        public boolean hasTerminalEvent(String runId) { return stores.hasTerminalEvent(runId); }
    }

    private record LocalRunQueue(LocalDevStores stores) implements RunQueue {
        public void enqueue(QueuedRun run) { stores.enqueue(run); }
        public Optional<QueuedRun> claimNext(String workerId, Instant now) { return stores.claimNext(); }
        public boolean markRunning(String runId, Instant startedAt) { return stores.markRunning(runId, startedAt); }
        public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) { return stores.markTerminalIfNotTerminal(runId, terminalStatus, Map.of(), Map.of(), finishedAt); }
        public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) { return stores.cancelQueuedAndReturn(runId); }
        public boolean removeIfTerminal(String runId) { return stores.removeIfTerminal(runId); }
    }
}
