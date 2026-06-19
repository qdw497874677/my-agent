package io.github.pi_java.agent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class TraceIdMigrationTest {

    private static final String LEGACY_TRACE_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String MIGRATED_TRACE_ID = "123e4567e89b12d3a456426614174000";
    private static final String W3C_TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String CORRELATION_ID = "corr-unchanged-123";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).target("2").load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void flywayMigratesLegacyTraceIdsWithoutChangingCorrelationIds() {
        insertLegacyAndW3cRows();

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertTraceIds("runs", "run_id", "run-legacy");
        assertTraceIds("run_events", "event_id", "event-legacy");
        assertTraceIds("audit_records", "resource_id", "resource-legacy");
        assertTraceIds("run_queue", "run_id", "queue-legacy");
    }

    private void assertTraceIds(String table, String idColumn, String legacyId) {
        assertThat(jdbcTemplate.queryForObject("select trace_id from " + table + " where " + idColumn + " = ?", String.class, legacyId))
                .isEqualTo(MIGRATED_TRACE_ID);
        assertThat(jdbcTemplate.queryForObject("select trace_id from " + table + " where " + idColumn + " = ?", String.class,
                legacyId.replace("legacy", "w3c"))).isEqualTo(W3C_TRACE_ID);
        assertThat(jdbcTemplate.queryForObject("select correlation_id from " + table + " where " + idColumn + " = ?", String.class, legacyId))
                .isEqualTo(CORRELATION_ID);
    }

    private void insertLegacyAndW3cRows() {
        jdbcTemplate.update("""
                insert into sessions (tenant_id, user_id, session_id, workspace_id, status, created_at, updated_at)
                values ('tenant-1', 'user-1', 'session-legacy', 'workspace-1', 'ACTIVE', ?, ?),
                       ('tenant-1', 'user-1', 'session-w3c', 'workspace-1', 'ACTIVE', ?, ?)
                """, Instant.parse("2026-06-19T00:00:00Z"), Instant.parse("2026-06-19T00:00:00Z"),
                Instant.parse("2026-06-19T00:00:00Z"), Instant.parse("2026-06-19T00:00:00Z"));
        insertRun("run-legacy", "session-legacy", LEGACY_TRACE_ID);
        insertRun("run-w3c", "session-w3c", W3C_TRACE_ID);
        insertRunEvent("event-legacy", "run-legacy", "session-legacy", LEGACY_TRACE_ID, 1);
        insertRunEvent("event-w3c", "run-w3c", "session-w3c", W3C_TRACE_ID, 1);
        insertAuditRecord("00000000-0000-0000-0000-000000000001", "resource-legacy", LEGACY_TRACE_ID);
        insertAuditRecord("00000000-0000-0000-0000-000000000002", "resource-w3c", W3C_TRACE_ID);
        insertRunQueue("queue-legacy", LEGACY_TRACE_ID);
        insertRunQueue("queue-w3c", W3C_TRACE_ID);
    }

    private void insertRun(String runId, String sessionId, String traceId) {
        jdbcTemplate.update("""
                insert into runs (run_id, session_id, tenant_id, user_id, workspace_id, status, input_type, input, trace_id, correlation_id, created_at, updated_at)
                values (?, ?, 'tenant-1', 'user-1', 'workspace-1', 'QUEUED', 'chat', '{}'::jsonb, ?, ?, ?, ?)
                """, runId, sessionId, traceId, CORRELATION_ID, Instant.parse("2026-06-19T00:00:00Z"), Instant.parse("2026-06-19T00:00:00Z"));
    }

    private void insertRunEvent(String eventId, String runId, String sessionId, String traceId, int sequence) {
        jdbcTemplate.update("""
                insert into run_events (event_id, run_id, session_id, tenant_id, user_id, workspace_id, step_id, sequence, event_type,
                    timestamp, trace_id, correlation_id, causation_id, visibility, payload_schema, payload_version)
                values (?, ?, ?, 'tenant-1', 'user-1', 'workspace-1', 'step-1', ?, 'run.created', ?, ?, ?, 'cause-1', 'USER', 'test', 1)
                """, eventId, runId, sessionId, sequence, Instant.parse("2026-06-19T00:00:00Z"), traceId, CORRELATION_ID);
    }

    private void insertAuditRecord(String auditId, String resourceId, String traceId) {
        jdbcTemplate.update("""
                insert into audit_records (audit_id, tenant_id, user_id, action, resource_type, resource_id, run_id, session_id, trace_id, correlation_id, timestamp)
                values (?::uuid, 'tenant-1', 'user-1', 'test.action', 'test', ?, 'run-legacy', 'session-legacy', ?, ?, ?)
                """, auditId, resourceId, traceId, CORRELATION_ID, Instant.parse("2026-06-19T00:00:00Z"));
    }

    private void insertRunQueue(String runId, String traceId) {
        jdbcTemplate.update("""
                insert into run_queue (run_id, session_id, tenant_id, user_id, workspace_id, trace_id, correlation_id, input_type, input, status, available_at, created_at, updated_at)
                values (?, 'session-legacy', 'tenant-1', 'user-1', 'workspace-1', ?, ?, 'chat', '{}'::jsonb, 'QUEUED', ?, ?, ?)
                """, runId, traceId, CORRELATION_ID, Instant.parse("2026-06-19T00:00:00Z"), Instant.parse("2026-06-19T00:00:00Z"),
                Instant.parse("2026-06-19T00:00:00Z"));
    }
}
