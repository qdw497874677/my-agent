package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 16 Plan 03 ownership/ordering integration tests for the production JDBC
 * conversation read-model queries (decisions D-09, D-11, D-12, D-15; pitfalls
 * #11/#15/#17; requirements SESS-01, SESS-04).
 *
 * <p>Seeds two tenants/users/sessions/runs/events and proves:
 * <ul>
 *   <li>recent sessions are tenant/user filtered and ordered by latest activity;</li>
 *   <li>transcript event loads cannot cross tenant/user/session boundaries even
 *       when a runId collides;</li>
 *   <li>session-owned run list returns input maps needed for USER transcript
 *       messages; and</li>
 *   <li>recent preview/status is derivable from typed {@link SessionSummaryDto}
 *       without {@code SessionHistoryResponse.entries} maps.</li>
 * </ul>
 */
@Testcontainers
class JdbcConversationReadModelIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private JdbcTemplate jdbcTemplate;
    private JdbcSessionRepository sessionRepository;
    private JdbcRunProjectionRepository runProjectionRepository;
    private JdbcRunEventStore eventStore;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        sessionRepository = new JdbcSessionRepository(jdbcTemplate);
        runProjectionRepository = new JdbcRunProjectionRepository(jdbcTemplate);
        eventStore = new JdbcRunEventStore(jdbcTemplate);
    }

    @Test
    void recentSessionsFilterTenantUserAndOrderByLatestActivityDescending() {
        RequestContext ownerA = context("tenant-a", "user-a");
        RequestContext ownerB = context("tenant-b", "user-b");

        // Older session for A (activity at T1).
        sessionRepository.create(ownerA, new CreateSessionRequest("ws", Map.of()), "sess-a-old", Instant.parse("2026-06-01T00:00:00Z"));
        runProjectionRepository.createRun(ownerA, "sess-a-old", "run-a-old",
                new CreateRunRequest("agent", "chat", Map.of("text", "hello old"), "ws", Map.of()));
        touchRun("run-a-old", Instant.parse("2026-06-01T00:01:00Z"));

        // Newer session for A (activity at T2 > T1).
        sessionRepository.create(ownerA, new CreateSessionRequest("ws", Map.of()), "sess-a-new", Instant.parse("2026-06-02T00:00:00Z"));
        runProjectionRepository.createRun(ownerA, "sess-a-new", "run-a-new",
                new CreateRunRequest("agent", "chat", Map.of("text", "hello new"), "ws", Map.of()));
        touchRun("run-a-new", Instant.parse("2026-06-02T00:01:00Z"));

        // Foreign session for B (must be excluded from A's recent list even though it is newest).
        sessionRepository.create(ownerB, new CreateSessionRequest("ws", Map.of()), "sess-b-foreign", Instant.parse("2026-06-03T00:00:00Z"));
        runProjectionRepository.createRun(ownerB, "sess-b-foreign", "run-b-foreign",
                new CreateRunRequest("agent", "chat", Map.of("text", "foreign"), "ws", Map.of()));
        touchRun("run-b-foreign", Instant.parse("2026-06-03T00:01:00Z"));

        PageResponse<SessionSummaryDto> page = sessionRepository.listRecent(ownerA, 10, null);

        assertThat(page.items()).extracting(SessionSummaryDto::sessionId)
                .containsExactly("sess-a-new", "sess-a-old")
                .doesNotContain("sess-b-foreign");
    }

    @Test
    void transcriptEventLoadExcludesForeignTenantUserSessionEvenWithSameRunId() {
        RequestContext ownerA = context("tenant-a", "user-a");
        RequestContext ownerB = context("tenant-b", "user-b");

        sessionRepository.create(ownerA, new CreateSessionRequest("ws", Map.of()), "sess-a", Instant.parse("2026-06-01T00:00:00Z"));
        sessionRepository.create(ownerB, new CreateSessionRequest("ws", Map.of()), "sess-b", Instant.parse("2026-06-01T00:00:00Z"));

        // Both owners create a run with a colliding runId but different sessions.
        runProjectionRepository.createRun(ownerA, "sess-a", "collide-run",
                new CreateRunRequest("agent", "chat", Map.of("text", "a"), "ws", Map.of()));
        runProjectionRepository.createRun(ownerB, "sess-b", "collide-run",
                new CreateRunRequest("agent", "chat", Map.of("text", "b"), "ws", Map.of()));

        eventStore.append(event("e-a-1", "tenant-a", "user-a", "sess-a", "collide-run", 1, RunEventType.RUN_STARTED));
        eventStore.append(event("e-b-1", "tenant-b", "user-b", "sess-b", "collide-run", 1, RunEventType.RUN_STARTED));
        eventStore.append(event("e-b-2", "tenant-b", "user-b", "sess-b", "collide-run", 2, RunEventType.MODEL_DELTA));

        List<RunEvent> aEvents = eventStore.listBySessionRun(ownerA, "sess-a", "collide-run", 0L, 10);

        assertThat(aEvents).extracting(RunEvent::eventId).containsExactly("e-a-1");
        assertThat(aEvents).allSatisfy(event -> {
            assertThat(event.tenantId().value()).isEqualTo("tenant-a");
            assertThat(event.sessionId().value()).isEqualTo("sess-a");
        });

        // The diagnostic path still returns everything for the runId (D-14 compatibility).
        assertThat(eventStore.listByRun("collide-run", 0L, 10)).hasSize(3);
    }

    @Test
    void listRunsBySessionReturnsRunInputMapsForTranscriptUserMessages() {
        RequestContext owner = context("tenant-a", "user-a");
        sessionRepository.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a", Instant.parse("2026-06-01T00:00:00Z"));
        runProjectionRepository.createRun(owner, "sess-a", "run-1",
                new CreateRunRequest("agent", "chat", Map.of("text", "what is pi?"), "ws", Map.of()));
        runProjectionRepository.createRun(owner, "sess-a", "run-2",
                new CreateRunRequest("agent", "chat", Map.of("prompt", "follow up"), "ws", Map.of()));

        PageResponse<ConversationRunView> page = runProjectionRepository.listRunsBySession(owner, "sess-a", 10, null);

        assertThat(page.items()).extracting(ConversationRunView::runId).containsExactly("run-1", "run-2");
        ConversationRunView first = page.items().get(0);
        assertThat(first.input()).containsEntry("text", "what is pi?");
        assertThat(first.status()).isEqualTo("QUEUED");
        ConversationRunView second = page.items().get(1);
        assertThat(second.input()).containsEntry("prompt", "follow up");
    }

    @Test
    void recentSessionSummaryDerivesTitlePreviewAndStatusWithoutHistoryEntries() {
        RequestContext owner = context("tenant-a", "user-a");
        sessionRepository.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a", Instant.parse("2026-06-01T00:00:00Z"));
        // First (oldest) run input becomes the stable title (D-10).
        runProjectionRepository.createRun(owner, "sess-a", "run-first",
                new CreateRunRequest("agent", "chat", Map.of("text", "first user question"), "ws", Map.of()));
        // Latest run input becomes the latest safe preview (D-11).
        runProjectionRepository.createRun(owner, "sess-a", "run-latest",
                new CreateRunRequest("agent", "chat", Map.of("text", "latest user question"), "ws", Map.of()));
        touchRun("run-latest", Instant.parse("2026-06-05T00:00:00Z"));

        PageResponse<SessionSummaryDto> page = sessionRepository.listRecent(owner, 10, null);
        assertThat(page.items()).hasSize(1);
        SessionSummaryDto summary = page.items().get(0);

        // Title derived from the FIRST user message, not retitled per prompt (D-10).
        assertThat(summary.title()).isEqualTo("first user question");
        // Latest safe preview derived from the latest run input (D-11).
        assertThat(summary.lastMessagePreview()).isEqualTo("latest user question");
        // Status derived from session/run state without SessionHistoryResponse.entries maps (D-12, D-13).
        assertThat(summary.status()).isEqualTo("ACTIVE");
        assertThat(summary.activeRunId()).isEqualTo("run-latest");
        assertThat(summary.activeRunStatus()).isEqualTo("QUEUED");
    }

    private void touchRun(String runId, Instant updatedAt) {
        jdbcTemplate.update("UPDATE runs SET updated_at = ? WHERE run_id = ?",
                java.sql.Timestamp.from(updatedAt), runId);
    }

    private static RequestContext context(String tenantId, String userId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.of()),
                new CorrelationContext(trace(), "correlation-" + tenantId, "causation-" + tenantId));
    }

    private static String trace() {
        return "0123456789abcdef0123456789abcdef";
    }

    private static RunEvent event(String eventId, String tenantId, String userId, String sessionId,
                                  String runId, long sequence, RunEventType type) {
        return new RunEvent(
                eventId,
                new TenantId(tenantId),
                new UserId(userId),
                new SessionId(sessionId),
                new RunId(runId),
                new StepId("step-none"),
                new WorkspaceId("ws"),
                sequence,
                Instant.parse("2026-06-01T00:00:00Z").plusSeconds(sequence),
                type,
                new TraceId(trace()),
                new CorrelationId("correlation-" + tenantId),
                new CausationId("causation-" + tenantId),
                new RunEventPayload.ExtensionPayload("test.event", "1", Map.of("sequence", sequence)),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "none"));
    }
}
