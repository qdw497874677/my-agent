package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class JdbcAuditRepository implements AuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(RequestContext context, String action, String resourceType, String resourceId, String sessionId, String runId, Map<String, Object> details) {
        jdbcTemplate.update("""
                        INSERT INTO audit_records(audit_id, tenant_id, user_id, action, resource_type, resource_id, run_id, session_id,
                            trace_id, correlation_id, timestamp, details)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                context.tenantId(),
                context.userId(),
                action,
                resourceType,
                resourceId,
                runId,
                sessionId,
                context.traceId(),
                context.correlationId(),
                Timestamp.from(Instant.now()),
                JdbcJson.jsonb(details));
    }
}
