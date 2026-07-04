package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcRunProjectionRepository implements RunProjectionRepository {

    private static final String TERMINAL_STATUSES = "'COMPLETED','FAILED','CANCELLED','TIMED_OUT','POLICY_BLOCKED'";

    private final JdbcTemplate jdbcTemplate;

    public JdbcRunProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                        INSERT INTO runs(run_id, session_id, tenant_id, user_id, workspace_id, status, input_type, input,
                            terminal_result, failure, trace_id, correlation_id, created_at, updated_at, provider_metadata)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                runId,
                sessionId,
                context.tenantId(),
                context.userId(),
                workspaceId(request),
                "QUEUED",
                request.inputType(),
                JdbcJson.jsonb(request.input()),
                null,
                null,
                context.traceId(),
                context.correlationId(),
                Timestamp.from(now),
                Timestamp.from(now),
                JdbcJson.jsonb(toProviderMetadataMap(safeProviderMetadata(request.metadata()))));
    }

    @Override
    public Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId) {
        return jdbcTemplate.query("""
                        SELECT * FROM runs
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ? AND run_id = ?
                        """,
                runRowMapper(), context.tenantId(), context.userId(), sessionId, runId).stream().findFirst();
    }

    @Override
    public RunStatusResponse getStatus(RequestContext context, String sessionId, String runId) {
        return jdbcTemplate.queryForObject("""
                        SELECT session_id, run_id, status, updated_at, trace_id, correlation_id FROM runs
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ? AND run_id = ?
                        """,
                (rs, rowNum) -> new RunStatusResponse(
                        rs.getString("session_id"),
                        rs.getString("run_id"),
                        rs.getString("status"),
                        isTerminal(rs.getString("status")),
                        rs.getTimestamp("updated_at").toInstant(),
                        rs.getString("trace_id"),
                        rs.getString("correlation_id")),
                context.tenantId(), context.userId(), sessionId, runId);
    }

    @Override
    public boolean markRunning(String runId, Instant startedAt) {
        int updated = jdbcTemplate.update("""
                        UPDATE runs SET status = 'RUNNING', started_at = ?, updated_at = ?
                        WHERE run_id = ? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','TIMED_OUT','POLICY_BLOCKED')
                        """,
                Timestamp.from(startedAt), Timestamp.from(startedAt), runId);
        return updated > 0;
    }

    @Override
    public boolean requestCancellation(String runId, String reason, Instant requestedAt) {
        int updated = jdbcTemplate.update("""
                        UPDATE runs SET status = 'CANCELLING', cancel_requested_at = ?, cancel_reason = ?, updated_at = ?
                        WHERE run_id = ? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','TIMED_OUT','POLICY_BLOCKED')
                        """,
                Timestamp.from(requestedAt), reason, Timestamp.from(requestedAt), runId);
        return updated > 0;
    }

    @Override
    public boolean markTerminalIfNotTerminal(String runId, String status, Map<String, Object> terminalResult, Map<String, Object> failure, Instant finishedAt) {
        int updated = jdbcTemplate.update("""
                        UPDATE runs SET status = ?, terminal_result = ?, failure = ?, finished_at = ?, updated_at = ?
                        WHERE run_id = ? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','TIMED_OUT','POLICY_BLOCKED')
                        """,
                status,
                terminalResult == null ? null : JdbcJson.jsonb(terminalResult),
                failure == null ? null : JdbcJson.jsonb(failure),
                Timestamp.from(finishedAt),
                Timestamp.from(finishedAt),
                runId);
        return updated > 0;
    }

    @Override
    public void updateLastEventSequence(String runId, long sequence, Instant updatedAt) {
        jdbcTemplate.update("""
                        UPDATE runs SET last_event_sequence = GREATEST(last_event_sequence, ?), updated_at = ?
                        WHERE run_id = ?
                        """,
                sequence, Timestamp.from(updatedAt), runId);
    }

    @Override
    public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
        RunResponse run = findRun(context, sessionId, runId).orElseThrow();
        return new RunDetailResponse(run, List.of(), listSteps(context, sessionId, runId, 100).items(), listMessages(context, sessionId, runId, 100).items(), listToolCalls(context, sessionId, runId, 100).items(), getRunResult(context, sessionId, runId));
    }

    @Override
    public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
        return page(jdbcTemplate.queryForList("SELECT * FROM steps WHERE run_id = ? ORDER BY created_at ASC LIMIT ?", runId, limit), limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
        return page(jdbcTemplate.queryForList("SELECT * FROM messages WHERE run_id = ? ORDER BY created_at ASC LIMIT ?", runId, limit), limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
        return page(jdbcTemplate.queryForList("SELECT * FROM tool_calls WHERE run_id = ? ORDER BY created_at ASC LIMIT ?", runId, limit), limit);
    }

    @Override
    public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
        return jdbcTemplate.queryForObject("""
                        SELECT run_id, status, terminal_result, failure FROM runs
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ? AND run_id = ?
                        """,
                (rs, rowNum) -> new RunResultResponse(rs.getString("run_id"), rs.getString("status"), JdbcJson.readMap(rs.getObject("terminal_result")), JdbcJson.readMap(rs.getObject("failure"))),
                context.tenantId(), context.userId(), sessionId, runId);
    }

    /**
     * Ownership-aware, session-scoped run query for the conversation transcript
     * assembler (decisions D-09, D-15, D-16; requirement SESS-04).
     *
     * <p>Filters by tenant/user/session at SQL level and orders by run creation
     * ascending for stable transcript assembly. Each {@link ConversationRunView}
     * carries the persisted input map (source of the USER message text) and run
     * status; this path does not require Vaadin or in-memory state and does not
     * touch the diagnostic {@code SessionHistoryResponse} entries (D-13, D-14).
     */
    @Override
    public PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor) {
        int pageSize = limit > 0 ? limit : 20;
        int fetchSize = pageSize + 1;
        List<ConversationRunView> rows = jdbcTemplate.query("""
                        SELECT run_id, created_at, input, status, provider_metadata FROM runs
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ?
                        ORDER BY created_at ASC, run_id ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ConversationRunView(
                        rs.getString("run_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        JdbcJson.readMap(rs.getObject("input")),
                        rs.getString("status"),
                        providerMetadata(rs.getObject("provider_metadata"))),
                context.tenantId(), context.userId(), sessionId, fetchSize);

        boolean hasMore = rows.size() > pageSize;
        List<ConversationRunView> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
        return new PageResponse<>(page, pageSize, null, null, hasMore);
    }

    private static PageResponse<Map<String, Object>> page(List<Map<String, Object>> items, int limit) {
        return new PageResponse<>(items, limit, null, null, false);
    }

    private static RowMapper<RunResponse> runRowMapper() {
        return (rs, rowNum) -> new RunResponse(
                rs.getString("tenant_id"),
                rs.getString("user_id"),
                rs.getString("session_id"),
                rs.getString("run_id"),
                rs.getString("workspace_id"),
                rs.getString("status"),
                rs.getString("trace_id"),
                rs.getString("correlation_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                providerMetadata(rs.getObject("provider_metadata")));
    }

    private static RunProviderMetadata providerMetadata(Object value) {
        Map<String, Object> metadata = JdbcJson.readMap(value);
        return new RunProviderMetadata(
                string(metadata.get("requestedModelRef")),
                string(metadata.get("selectedModelRef")),
                string(metadata.get("resolvedProviderId")),
                string(metadata.get("resolvedModelId")),
                string(metadata.get("fallbackMode")),
                string(metadata.get("readinessState")),
                string(metadata.get("safeErrorSummary")));
    }

    private static Map<String, Object> toProviderMetadataMap(RunProviderMetadata metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
        putIfPresent(values, "requestedModelRef", metadata.requestedModelRef());
        putIfPresent(values, "selectedModelRef", metadata.selectedModelRef());
        putIfPresent(values, "resolvedProviderId", metadata.resolvedProviderId());
        putIfPresent(values, "resolvedModelId", metadata.resolvedModelId());
        putIfPresent(values, "fallbackMode", metadata.fallbackMode());
        putIfPresent(values, "readinessState", metadata.readinessState());
        putIfPresent(values, "safeErrorSummary", metadata.safeErrorSummary());
        return values;
    }

    public static RunProviderMetadata safeProviderMetadata(Map<String, Object> metadata) {
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

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
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

    private static boolean isTerminal(String status) {
        return List.of("COMPLETED", "FAILED", "CANCELLED", "TIMED_OUT", "POLICY_BLOCKED").contains(status);
    }

    private static String workspaceId(CreateRunRequest request) {
        return request.workspaceId() == null || request.workspaceId().isBlank() ? "default" : request.workspaceId();
    }
}
