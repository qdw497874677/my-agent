package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JdbcRunEventStore implements RunEventStore {

    private static final Set<String> TERMINAL_EVENT_TYPES = Set.of(
            "run.completed",
            "run.failed",
            "run.cancelled",
            "run.policy_blocked");

    private final JdbcTemplate jdbcTemplate;

    public JdbcRunEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(RunEvent event) {
        jdbcTemplate.update("""
                        INSERT INTO run_events(event_id, run_id, session_id, tenant_id, user_id, workspace_id, step_id, sequence,
                            event_type, timestamp, trace_id, correlation_id, causation_id, visibility, redaction,
                            payload_schema, payload_version, payload)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                event.eventId(),
                event.runId().value(),
                event.sessionId().value(),
                event.tenantId().value(),
                event.userId().value(),
                event.workspaceId().value(),
                event.stepId().value(),
                event.sequence(),
                event.type().wireName(),
                Timestamp.from(event.timestamp()),
                event.traceId().value(),
                event.correlationId().value(),
                event.causationId().value(),
                event.visibility().name(),
                JdbcJson.redaction(event.redaction().containsSecrets(), event.redaction().redacted(), event.redaction().redactedFields(), event.redaction().policyRef()),
                payloadSchema(event.payload()),
                payloadVersion(event.payload()),
                JdbcJson.jsonb(payloadBody(event.payload())));
    }

    @Override
    public List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
        return jdbcTemplate.query("""
                        SELECT * FROM run_events
                        WHERE run_id = ? AND sequence > ?
                        ORDER BY sequence ASC
                        LIMIT ?
                        """,
                rowMapper(), runId, afterSequence, limit);
    }

    /**
     * Ownership-safe transcript event read path (decisions D-05, D-14, D-15;
     * requirement SESS-04).
     *
     * <p>Unlike the diagnostic {@link #listByRun(String, long, int)}, this
     * method carries the {@link RequestContext} plus both session and run
     * identifiers so tenant/user/session/run ownership filters are enforced at
     * the SQL layer before audit/replay events are returned to the conversation
     * assembler. Events arrive in ascending sequence order.
     */
    @Override
    public List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
        return jdbcTemplate.query("""
                        SELECT * FROM run_events
                        WHERE tenant_id = ? AND user_id = ? AND session_id = ? AND run_id = ? AND sequence > ?
                        ORDER BY sequence ASC
                        LIMIT ?
                        """,
                rowMapper(),
                context.tenantId(), context.userId(), sessionId, runId, afterSequence, limit);
    }

    @Override
    public Optional<RunEvent> findLastByRun(String runId) {
        List<RunEvent> events = jdbcTemplate.query("""
                        SELECT * FROM run_events
                        WHERE run_id = ?
                        ORDER BY sequence DESC
                        LIMIT 1
                        """,
                rowMapper(), runId);
        return events.stream().findFirst();
    }

    @Override
    public boolean hasTerminalEvent(String runId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT count(*) FROM run_events
                        WHERE run_id = ? AND event_type in ('run.completed','run.failed','run.cancelled','run.policy_blocked')
                        """,
                Integer.class, runId);
        return count != null && count > 0;
    }

    private static RowMapper<RunEvent> rowMapper() {
        return (rs, rowNum) -> new RunEvent(
                rs.getString("event_id"),
                new TenantId(rs.getString("tenant_id")),
                new UserId(rs.getString("user_id")),
                new SessionId(rs.getString("session_id")),
                new RunId(rs.getString("run_id")),
                new StepId(rs.getString("step_id")),
                new WorkspaceId(rs.getString("workspace_id")),
                rs.getLong("sequence"),
                rs.getTimestamp("timestamp").toInstant(),
                typeFromWireName(rs.getString("event_type")),
                new TraceId(rs.getString("trace_id")),
                new CorrelationId(rs.getString("correlation_id")),
                new CausationId(rs.getString("causation_id")),
                new RunEventPayload.ExtensionPayload(rs.getString("payload_schema"), String.valueOf(rs.getInt("payload_version")), JdbcJson.readMap(rs.getObject("payload"))),
                EventVisibility.valueOf(rs.getString("visibility")),
                new RedactionMetadata(false, false, Set.of(), "none"));
    }

    private static RunEventType typeFromWireName(String wireName) {
        for (RunEventType type : RunEventType.values()) {
            if (type.wireName().equals(wireName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown run event type: " + wireName);
    }

    private static String payloadSchema(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.schema();
        }
        return payload.getClass().getSimpleName();
    }

    private static int payloadVersion(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return Integer.parseInt(extensionPayload.version());
        }
        return 1;
    }

    private static Map<String, Object> payloadBody(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.attributes();
        }
        return Map.of("kind", payload.getClass().getSimpleName());
    }
}
