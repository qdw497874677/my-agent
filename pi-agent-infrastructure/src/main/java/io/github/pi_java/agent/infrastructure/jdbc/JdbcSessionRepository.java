package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) {
        String workspaceId = request.workspaceId() == null || request.workspaceId().isBlank() ? "default" : request.workspaceId();
        jdbcTemplate.update("""
                        INSERT INTO sessions(tenant_id, user_id, session_id, workspace_id, current_entry_id, status, created_at, updated_at, metadata)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                context.tenantId(), context.userId(), sessionId, workspaceId, null, "ACTIVE",
                Timestamp.from(now), Timestamp.from(now), JdbcJson.jsonb(request.metadata()));
        return new SessionResponse(context.tenantId(), context.userId(), sessionId, workspaceId, null, "ACTIVE", now, now,
                request.metadata() == null ? Map.of() : request.metadata());
    }

    @Override
    public Optional<SessionResponse> findById(RequestContext context, String sessionId) {
        List<SessionResponse> sessions = jdbcTemplate.query("""
                        SELECT * FROM sessions
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ?
                        """,
                rowMapper(), context.tenantId(), context.userId(), sessionId);
        return sessions.stream().findFirst();
    }

    @Override
    public SessionHistoryResponse history(RequestContext context, String sessionId) {
        SessionResponse session = findById(context, sessionId).orElseThrow();
        List<Map<String, Object>> entries = jdbcTemplate.queryForList("""
                        SELECT entry_id, session_id, parent_entry_id, sequence, payload_type, payload, created_at
                        FROM session_entries
                        WHERE session_id = ?
                        ORDER BY sequence ASC
                        """,
                sessionId);
        return new SessionHistoryResponse(session, entries);
    }

    private static RowMapper<SessionResponse> rowMapper() {
        return (rs, rowNum) -> new SessionResponse(
                rs.getString("tenant_id"),
                rs.getString("user_id"),
                rs.getString("session_id"),
                rs.getString("workspace_id"),
                rs.getString("current_entry_id"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                JdbcJson.readMap(rs.getObject("metadata")));
    }
}
