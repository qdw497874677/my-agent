package io.github.pi_java.agent.infrastructure.queue;

import io.github.pi_java.agent.app.port.execution.QueuedRun;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresRunQueueTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private JdbcTemplate jdbcTemplate;
    private PostgresRunQueue queue;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        queue = new PostgresRunQueue(jdbcTemplate);
    }

    @Test
    void claimNextUsesSkipLockedAndMarksClaimed() {
        queue.enqueue(queuedRun("run-1", Instant.parse("2026-06-14T00:00:00Z")));

        QueuedRun claimed = queue.claimNext("worker-1", Instant.parse("2026-06-14T00:00:01Z")).orElseThrow();

        assertThat(claimed.runId()).isEqualTo("run-1");
        Map<String, Object> row = jdbcTemplate.queryForMap("select status, lease_owner, lease_until, attempt_count from run_queue where run_id = ?", "run-1");
        assertThat(row).containsEntry("status", "CLAIMED").containsEntry("lease_owner", "worker-1").containsEntry("attempt_count", 1);
        assertThat(row.get("lease_until")).isNotNull();
    }

    @Test
    void cancelQueuedAndReturnReturnsQueuedPayloadAndPreventsClaim() {
        queue.enqueue(queuedRun("run-2", Instant.parse("2026-06-14T00:00:00Z")));

        QueuedRun cancelled = queue.cancelQueuedAndReturn("run-2", "user", Instant.parse("2026-06-14T00:00:01Z")).orElseThrow();

        assertThat(cancelled.runId()).isEqualTo("run-2");
        assertThat(cancelled.input()).containsEntry("text", "run-2");
        assertThat(queue.claimNext("worker-1", Instant.parse("2026-06-14T00:00:02Z"))).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select status from run_queue where run_id = ?", String.class, "run-2")).isEqualTo("CANCELLED");
    }

    @Test
    void cancelQueuedAndReturnReturnsEmptyForRunningOrTerminalRows() {
        queue.enqueue(queuedRun("run-3", Instant.parse("2026-06-14T00:00:00Z")));
        queue.claimNext("worker-1", Instant.parse("2026-06-14T00:00:01Z"));
        queue.markRunning("run-3", Instant.parse("2026-06-14T00:00:02Z"));
        queue.enqueue(queuedRun("run-4", Instant.parse("2026-06-14T00:00:00Z")));
        queue.markTerminal("run-4", "COMPLETED", Instant.parse("2026-06-14T00:00:02Z"));

        assertThat(queue.cancelQueuedAndReturn("run-3", "user", Instant.parse("2026-06-14T00:00:03Z"))).isEmpty();
        assertThat(queue.cancelQueuedAndReturn("run-4", "user", Instant.parse("2026-06-14T00:00:03Z"))).isEmpty();
        assertThat(queue.cancelQueuedAndReturn("missing", "user", Instant.parse("2026-06-14T00:00:03Z"))).isEmpty();
    }

    @Test
    void markRunningRequiresClaimedState() {
        queue.enqueue(queuedRun("run-5", Instant.parse("2026-06-14T00:00:00Z")));

        assertThat(queue.markRunning("run-5", Instant.parse("2026-06-14T00:00:01Z"))).isFalse();
        queue.claimNext("worker-1", Instant.parse("2026-06-14T00:00:01Z"));
        assertThat(queue.markRunning("run-5", Instant.parse("2026-06-14T00:00:02Z"))).isTrue();
    }

    @Test
    void markTerminalClearsLeaseAndIsIdempotent() {
        queue.enqueue(queuedRun("run-6", Instant.parse("2026-06-14T00:00:00Z")));
        queue.claimNext("worker-1", Instant.parse("2026-06-14T00:00:01Z"));

        assertThat(queue.markTerminal("run-6", "FAILED", Instant.parse("2026-06-14T00:00:02Z"))).isTrue();
        assertThat(queue.markTerminal("run-6", "COMPLETED", Instant.parse("2026-06-14T00:00:03Z"))).isFalse();
        Map<String, Object> row = jdbcTemplate.queryForMap("select status, lease_owner, lease_until from run_queue where run_id = ?", "run-6");
        assertThat(row).containsEntry("status", "FAILED").containsEntry("lease_owner", null).containsEntry("lease_until", null);
    }

    @Test
    void removeIfTerminalOnlyDeletesTerminalRows() {
        queue.enqueue(queuedRun("run-7", Instant.parse("2026-06-14T00:00:00Z")));
        queue.enqueue(queuedRun("run-8", Instant.parse("2026-06-14T00:00:00Z")));
        queue.markTerminal("run-8", "CANCELLED", Instant.parse("2026-06-14T00:00:01Z"));

        assertThat(queue.removeIfTerminal("run-7")).isFalse();
        assertThat(queue.removeIfTerminal("run-8")).isTrue();
        assertThat(jdbcTemplate.queryForObject("select count(*) from run_queue where run_id = ?", Integer.class, "run-7")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from run_queue where run_id = ?", Integer.class, "run-8")).isZero();
    }

    private static QueuedRun queuedRun(String runId, Instant availableAt) {
        return new QueuedRun(runId, "session-1", "tenant-1", "user-1", "workspace-1", "trace-1", "correlation-1", "chat", Map.of("text", runId), availableAt, 0);
    }
}
