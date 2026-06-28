package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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

    /**
     * Ownership-aware recent-session summary query (decisions D-09, D-10, D-11,
     * D-12, D-15; requirement SESS-01).
     *
     * <p>Filters {@code sessions} by tenant/user at SQL level and orders by the
     * latest conversation activity descending. Per run, the first user input
     * becomes the stable title fallback (D-10) and the most recent run input
     * becomes the latest safe preview (D-11); the session status and the latest
     * run status are surfaced so callers can derive recent status without
     * touching {@link SessionHistoryResponse} map entries (D-13). Title is
     * derived from the earliest run and is not retitled per prompt (D-10).
     */
    @Override
    public PageResponse<SessionSummaryDto> listRecent(RequestContext context, int limit, String cursor) {
        int pageSize = limit > 0 ? limit : 20;
        int fetchSize = pageSize + 1;
        List<SessionSummaryDto> rows = jdbcTemplate.query("""
                        SELECT s.session_id, s.status, s.created_at, s.updated_at, s.metadata,
                               first_input.input_json AS first_input,
                               latest_run.input_json AS latest_input,
                               latest_run.run_id AS latest_run_id,
                               latest_run.status AS latest_run_status,
                               latest_run.updated_at AS latest_run_updated_at
                        FROM sessions s
                        LEFT JOIN LATERAL (
                            SELECT input AS input_json FROM runs r
                            WHERE r.session_id = s.session_id
                              AND r.tenant_id = s.tenant_id
                              AND r.user_id = s.user_id
                            ORDER BY r.created_at ASC LIMIT 1
                        ) first_input ON true
                        LEFT JOIN LATERAL (
                            SELECT run_id, input AS input_json, status, updated_at FROM runs r
                            WHERE r.session_id = s.session_id
                              AND r.tenant_id = s.tenant_id
                              AND r.user_id = s.user_id
                            ORDER BY r.created_at DESC LIMIT 1
                        ) latest_run ON true
                        WHERE s.tenant_id = ? AND s.user_id = ?
                        ORDER BY COALESCE(latest_run.updated_at, s.updated_at) DESC, s.session_id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> toSummary(rs),
                context.tenantId(), context.userId(), fetchSize);

        boolean hasMore = rows.size() > pageSize;
        List<SessionSummaryDto> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
        return new PageResponse<>(page, pageSize, null, null, hasMore);
    }

    private static SessionSummaryDto toSummary(ResultSet rs) throws SQLException {
        String sessionId = rs.getString("session_id");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant sessionUpdatedAt = rs.getTimestamp("updated_at").toInstant();
        Instant latestRunUpdatedAt = rs.getTimestamp("latest_run_updated_at") != null
                ? rs.getTimestamp("latest_run_updated_at").toInstant() : null;
        Instant lastActivityAt = latestRunUpdatedAt != null ? latestRunUpdatedAt : sessionUpdatedAt;

        Map<String, Object> firstInput = JdbcJson.readMap(rs.getObject("first_input"));
        Map<String, Object> latestInput = JdbcJson.readMap(rs.getObject("latest_input"));

        String title = firstUserText(firstInput);
        if (title == null || title.isBlank()) {
            title = "New conversation";
        }
        String preview = firstUserText(latestInput);
        if (preview == null || preview.isBlank()) {
            preview = "";
        }

        String latestRunId = rs.getString("latest_run_id");
        String latestRunStatus = rs.getString("latest_run_status");

        return new SessionSummaryDto(
                sessionId,
                title,
                rs.getString("status"),
                preview,
                lastActivityAt,
                createdAt,
                latestRunId,
                latestRunStatus,
                JdbcJson.readMap(rs.getObject("metadata")));
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
