package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
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
                            terminal_result, failure, trace_id, correlation_id, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                Timestamp.from(now));
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
                rs.getTimestamp("updated_at").toInstant());
    }

    private static boolean isTerminal(String status) {
        return List.of("COMPLETED", "FAILED", "CANCELLED", "TIMED_OUT", "POLICY_BLOCKED").contains(status);
    }

    private static String workspaceId(CreateRunRequest request) {
        return request.workspaceId() == null || request.workspaceId().isBlank() ? "default" : request.workspaceId();
    }
}
