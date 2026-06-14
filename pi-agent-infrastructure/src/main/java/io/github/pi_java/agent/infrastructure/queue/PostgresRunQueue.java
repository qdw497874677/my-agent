package io.github.pi_java.agent.infrastructure.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class PostgresRunQueue implements RunQueue {

    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);
    private static final String TERMINAL_STATUSES = "'COMPLETED','FAILED','CANCELLED','TIMED_OUT','POLICY_BLOCKED'";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;

    public PostgresRunQueue(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void enqueue(QueuedRun run) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                        INSERT INTO run_queue(run_id, session_id, tenant_id, user_id, workspace_id, trace_id, correlation_id,
                            input_type, input, status, available_at, lease_owner, lease_until, attempt_count, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'QUEUED', ?, null, null, ?, ?, ?)
                        """,
                run.runId(),
                run.sessionId(),
                run.tenantId(),
                run.userId(),
                run.workspaceId(),
                run.traceId(),
                run.correlationId(),
                run.inputType(),
                jsonb(run.input()),
                Timestamp.from(run.availableAt()),
                run.attemptCount(),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    @Override
    public Optional<QueuedRun> claimNext(String workerId, Instant now) {
        Instant leaseUntil = now.plus(LEASE_DURATION);
        return jdbcTemplate.query("""
                        WITH next_run AS (
                            SELECT run_id
                            FROM run_queue
                            WHERE status = 'QUEUED' AND available_at <= ?
                            ORDER BY available_at ASC, created_at ASC
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        UPDATE run_queue q
                        SET status = 'CLAIMED', lease_owner = ?, lease_until = ?, attempt_count = q.attempt_count + 1, updated_at = ?
                        FROM next_run
                        WHERE q.run_id = next_run.run_id
                        RETURNING q.*
                        """,
                rowMapper(),
                Timestamp.from(now), workerId, Timestamp.from(leaseUntil), Timestamp.from(now)).stream().findFirst();
    }

    @Override
    public boolean markRunning(String runId, Instant startedAt) {
        int updated = jdbcTemplate.update("""
                        UPDATE run_queue SET status = 'RUNNING', updated_at = ?
                        WHERE run_id = ? AND status = 'CLAIMED'
                        """,
                Timestamp.from(startedAt), runId);
        return updated > 0;
    }

    @Override
    public boolean markTerminal(String runId, String terminalStatus, Instant finishedAt) {
        int updated = jdbcTemplate.update("""
                        UPDATE run_queue SET status = ?, lease_owner = null, lease_until = null, updated_at = ?
                        WHERE run_id = ? AND status NOT IN (COMPLETED, FAILED, CANCELLED, TIMED_OUT, POLICY_BLOCKED)
                        """.replace("COMPLETED, FAILED, CANCELLED, TIMED_OUT, POLICY_BLOCKED", TERMINAL_STATUSES),
                terminalStatus, Timestamp.from(finishedAt), runId);
        return updated > 0;
    }

    @Override
    public Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt) {
        return jdbcTemplate.query("""
                        UPDATE run_queue
                        SET status = 'CANCELLED', cancel_reason = ?, lease_owner = null, lease_until = null, updated_at = ?
                        WHERE run_id = ? AND status IN ('QUEUED','CLAIMED')
                        RETURNING *
                        """,
                rowMapper(), reason, Timestamp.from(cancelledAt), runId).stream().findFirst();
    }

    @Override
    public boolean removeIfTerminal(String runId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM run_queue
                        WHERE run_id = ? AND status IN (COMPLETED, FAILED, CANCELLED, TIMED_OUT, POLICY_BLOCKED)
                        """.replace("COMPLETED, FAILED, CANCELLED, TIMED_OUT, POLICY_BLOCKED", TERMINAL_STATUSES),
                runId);
        return updated > 0;
    }

    private static RowMapper<QueuedRun> rowMapper() {
        return (rs, rowNum) -> new QueuedRun(
                rs.getString("run_id"),
                rs.getString("session_id"),
                rs.getString("tenant_id"),
                rs.getString("user_id"),
                rs.getString("workspace_id"),
                rs.getString("trace_id"),
                rs.getString("correlation_id"),
                rs.getString("input_type"),
                readMap(rs.getObject("input")),
                rs.getTimestamp("available_at").toInstant(),
                rs.getInt("attempt_count"));
    }

    private static PGobject jsonb(Map<String, Object> value) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize JSONB value", ex);
        }
    }

    private static Map<String, Object> readMap(Object value) throws SQLException {
        if (value == null) {
            return Map.of();
        }
        try {
            String json = value instanceof PGobject pg ? pg.getValue() : value.toString();
            return json == null || json.isBlank() ? Map.of() : OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new SQLException("Failed to read JSONB map", ex);
        }
    }
}
