package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.run.CreateRunRequest;
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
import io.github.pi_java.agent.infrastructure.event.PersistingEventSink;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class JdbcPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcRunEventStore eventStore;
    private JdbcRunProjectionRepository runProjectionRepository;
    private JdbcSessionRepository sessionRepository;
    private JdbcAuditRepository auditRepository;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        eventStore = new JdbcRunEventStore(jdbcTemplate);
        runProjectionRepository = new JdbcRunProjectionRepository(jdbcTemplate);
        sessionRepository = new JdbcSessionRepository(jdbcTemplate);
        auditRepository = new JdbcAuditRepository(jdbcTemplate);
        transactionTemplate = new TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
    }

    @Test
    void flywayCreatesCloudRuntimeTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tables).contains(
                "sessions",
                "session_entries",
                "runs",
                "run_events",
                "steps",
                "messages",
                "tool_calls",
                "audit_records",
                "run_queue");
    }

    @Test
    void runEventsEnforcePerRunSequenceUniqueness() {
        eventStore.append(event("event-1", "run-1", 1, RunEventType.RUN_CREATED));

        assertThatThrownBy(() -> eventStore.append(event("event-2", "run-1", 1, RunEventType.RUN_STARTED)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void appendAndReplayRunEventsOrderedBySequence() {
        eventStore.append(event("event-2", "run-2", 2, RunEventType.MODEL_DELTA));
        eventStore.append(event("event-1", "run-2", 1, RunEventType.RUN_STARTED));

        List<RunEvent> replay = eventStore.listByRun("run-2", 0, 10);

        assertThat(replay).extracting(RunEvent::sequence).containsExactly(1L, 2L);
        assertThat(eventStore.findLastByRun("run-2")).get().extracting(RunEvent::sequence).isEqualTo(2L);
    }

    @Test
    void markTerminalIfNotTerminalIsIdempotent() {
        RequestContext context = requestContext();
        sessionRepository.create(context, new io.github.pi_java.agent.client.session.CreateSessionRequest("workspace-1", Map.of()), "session-1", Instant.parse("2026-06-14T00:00:00Z"));
        runProjectionRepository.createRun(context, "session-1", "run-3", new CreateRunRequest("agent-1", "chat", Map.of("text", "hi"), "workspace-1", Map.of()));

        boolean first = runProjectionRepository.markTerminalIfNotTerminal("run-3", "COMPLETED", Map.of("ok", true), null, Instant.parse("2026-06-14T00:01:00Z"));
        boolean second = runProjectionRepository.markTerminalIfNotTerminal("run-3", "FAILED", null, Map.of("message", "late"), Instant.parse("2026-06-14T00:02:00Z"));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(runProjectionRepository.getStatus(context, "session-1", "run-3").status()).isEqualTo("COMPLETED");
    }

    @Test
    void auditRecordsPersistCorrelationContext() {
        RequestContext context = requestContext();

        auditRepository.record(context, "run.cancel", "run", "run-4", "session-1", "run-4", Map.of("reason", "user"));

        Map<String, Object> row = jdbcTemplate.queryForMap("select * from audit_records where run_id = ?", "run-4");
        assertThat(row).containsEntry("tenant_id", "tenant-1");
        assertThat(row).containsEntry("user_id", "user-1");
        assertThat(row).containsEntry("trace_id", "0123456789abcdef0123456789abcdef");
        assertThat(row).containsEntry("correlation_id", "correlation-1");
    }

    @Test
    void persistingEventSinkDoesNotFanoutWhenAppendFails() {
        eventStore.append(event("event-5", "run-5", 1, RunEventType.RUN_CREATED));
        List<RunEvent> published = new ArrayList<>();
        PersistingEventSink sink = new PersistingEventSink(transactionTemplate, eventStore, runProjectionRepository, published::add);

        assertThatThrownBy(() -> sink.publish(event("event-6", "run-5", 1, RunEventType.RUN_STARTED)))
                .isInstanceOf(RuntimeException.class);

        assertThat(published).isEmpty();
    }

    @Test
    void persistingEventSinkPersistsBeforeFanout() {
        List<RunEvent> published = new ArrayList<>();
        PersistingEventSink sink = new PersistingEventSink(transactionTemplate, eventStore, runProjectionRepository, event -> {
            assertThat(eventStore.listByRun(event.runId().value(), 0, 10)).extracting(RunEvent::eventId).contains(event.eventId());
            published.add(event);
        });

        RunEvent event = event("event-7", "run-7", 1, RunEventType.RUN_COMPLETED);
        sink.publish(event);

        assertThat(published).containsExactly(event);
        assertThat(eventStore.hasTerminalEvent("run-7")).isTrue();
    }

    private static RequestContext requestContext() {
        return new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("0123456789abcdef0123456789abcdef", "correlation-1", "causation-1"));
    }

    private static RunEvent event(String eventId, String runId, long sequence, RunEventType type) {
        return new RunEvent(
                eventId,
                new TenantId("tenant-1"),
                new UserId("user-1"),
                new SessionId("session-1"),
                new RunId(runId),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                sequence,
                Instant.parse("2026-06-14T00:00:00Z").plusSeconds(sequence),
                type,
                new TraceId("0123456789abcdef0123456789abcdef"),
                new CorrelationId("correlation-1"),
                new CausationId("causation-1"),
                new RunEventPayload.ExtensionPayload("test.event", "1", Map.of("sequence", sequence)),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "none"));
    }
}
