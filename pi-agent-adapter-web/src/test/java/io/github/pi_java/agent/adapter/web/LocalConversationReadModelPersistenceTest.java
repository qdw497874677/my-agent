package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalDevStores;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunEventStore;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunProjectionRepository;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalSessionRepository;
import io.github.pi_java.agent.adapter.web.provider.SqliteLocalPersistence;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 16 Plan 03 contract tests for the minimal local-profile SQLite
 * read-model alignment (decision D-17; requirements SESS-01, SESS-04).
 *
 * <p>Verifies that {@link SqliteLocalPersistence} retains run input/ownership
 * columns, orders recent sessions by latest activity, and can load runs/events
 * with tenant/user/session filters; and that the local in-memory repositories
 * implement the same App repository ports as the JDBC layer without Vaadin
 * memory or Phase 20 restart UX claims.
 */
class LocalConversationReadModelPersistenceTest {

    @Test
    void sqlitePersistenceCreatesMissingParentDirectoryForLocalProfileDb(@TempDir Path tmp) {
        Path db = tmp.resolve("missing-parent").resolve("pi-local.db");

        SqliteLocalPersistence persistence = new SqliteLocalPersistence(db.toString());
        persistence.saveSession("sess-local", "tenant-a", "user-a", "ws", "ACTIVE",
                "2026-06-01T00:00:00Z", "2026-06-01T00:01:00Z", "{}");

        assertThat(db.getParent()).isDirectory();
        assertThat(db).exists();
        assertThat(persistence.loadSessions()).extracting(row -> row.get("session_id"))
                .containsExactly("sess-local");
    }

    @Test
    void localSessionsOrderedByUpdatedAtDescAndFilteredByTenantUser(@TempDir Path tmp) {
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(tmp.resolve("read-model.db").toString());

        persistence.saveSession("sess-a-old", "tenant-a", "user-a", "ws", "ACTIVE",
                "2026-06-01T00:00:00Z", "2026-06-01T00:01:00Z", "{}");
        persistence.saveSession("sess-a-new", "tenant-a", "user-a", "ws", "ACTIVE",
                "2026-06-02T00:00:00Z", "2026-06-02T00:01:00Z", "{}");
        persistence.saveSession("sess-b-foreign", "tenant-b", "user-b", "ws", "ACTIVE",
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", "{}");

        List<Map<String, String>> recent = persistence.loadRecentSessions("tenant-a", "user-a", 10);

        assertThat(recent).extracting(row -> row.get("session_id"))
                .containsExactly("sess-a-new", "sess-a-old")
                .doesNotContain("sess-b-foreign");

        List<Map<String, String>> limited = persistence.loadRecentSessions("tenant-a", "user-a", 1);
        assertThat(limited).hasSize(1);
        assertThat(limited.get(0).get("session_id")).isEqualTo("sess-a-new");
    }

    @Test
    void localRunsRetainInputJsonAndOwnershipForTranscriptUserMessages(@TempDir Path tmp) {
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(tmp.resolve("runs.db").toString());

        persistence.saveRun("run-1", "sess-a", "tenant-a", "user-a", "ws", "QUEUED",
                "2026-06-01T00:00:00Z", "2026-06-01T00:00:00Z", null, null,
                "{\"text\":\"what is pi?\"}", "trace-1", "correlation-1");
        persistence.saveRun("run-2", "sess-a", "tenant-a", "user-a", "ws", "QUEUED",
                "2026-06-02T00:00:00Z", "2026-06-02T00:00:00Z", null, null,
                "{\"prompt\":\"follow up\"}", "trace-2", "correlation-2");

        List<Map<String, String>> runs = persistence.loadRunsBySession("tenant-a", "user-a", "sess-a", 10);

        assertThat(runs).extracting(row -> row.get("run_id")).containsExactly("run-1", "run-2");
        assertThat(runs.get(0)).containsEntry("input_json", "{\"text\":\"what is pi?\"}");
        assertThat(runs.get(0)).containsEntry("trace_id", "trace-1");
        assertThat(runs.get(0)).containsEntry("correlation_id", "correlation-1");

        // Foreign tenant/user excluded.
        assertThat(persistence.loadRunsBySession("tenant-b", "user-b", "sess-a", 10)).isEmpty();
    }

    @Test
    void localEventsLoadableByRunAndBySessionRunWithSequenceOrdering(@TempDir Path tmp) {
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(tmp.resolve("events.db").toString());

        persistence.appendEvent("e-a-2", "run-shared", "tenant-a", "user-a", "sess-a",
                2, "MODEL_DELTA", "2026-06-01T00:00:02Z", "{}");
        persistence.appendEvent("e-a-1", "run-shared", "tenant-a", "user-a", "sess-a",
                1, "RUN_STARTED", "2026-06-01T00:00:01Z", "{}");
        persistence.appendEvent("e-b-1", "run-shared", "tenant-b", "user-b", "sess-b",
                1, "RUN_STARTED", "2026-06-01T00:00:01Z", "{}");

        // By run (diagnostic, no ownership filter).
        List<Map<String, String>> byRun = persistence.loadEventsByRun("run-shared", 0L, 10);
        assertThat(byRun).extracting(row -> row.get("event_id")).containsExactlyInAnyOrder("e-a-1", "e-a-2", "e-b-1");
        assertThat(byRun).extracting(row -> Long.parseLong(row.get("sequence"))).isSorted();

        // By session/run (ownership-safe): only tenant-a/session-a events.
        List<Map<String, String>> bySessionRun = persistence.loadEventsBySessionRun(
                "tenant-a", "user-a", "sess-a", "run-shared", 0L, 10);
        assertThat(bySessionRun).extracting(row -> row.get("event_id")).containsExactly("e-a-1", "e-a-2");

        // Foreign owner/session excluded.
        assertThat(persistence.loadEventsBySessionRun("tenant-b", "user-b", "sess-a", "run-shared", 0L, 10)).isEmpty();
    }

    @Test
    void localRepositoriesImplementSameAppPortsAsJdbcWithOwnershipFilters(@TempDir Path tmp) {
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(tmp.resolve("ports.db").toString());
        LocalDevStores stores = new LocalDevStores(persistence);
        stores.loadAll();

        LocalSessionRepository sessionRepository = new LocalSessionRepository(stores);
        LocalRunProjectionRepository runProjectionRepository = new LocalRunProjectionRepository(stores);
        LocalRunEventStore runEventStore = new LocalRunEventStore(stores);

        RequestContext ownerA = context("tenant-a", "user-a");
        RequestContext ownerB = context("tenant-b", "user-b");

        // Seed via the ports (mirrors runtime usage + round-trips to SQLite).
        sessionRepository.create(ownerA, new CreateSessionRequest("ws", Map.of()), "sess-a", Instant.parse("2026-06-01T00:00:00Z"));
        sessionRepository.create(ownerB, new CreateSessionRequest("ws", Map.of()), "sess-b", Instant.parse("2026-06-01T00:00:00Z"));

        runProjectionRepository.createRun(ownerA, "sess-a", "run-a",
                new CreateRunRequest("agent", "chat", Map.of("text", "hello a"), "ws", Map.of()));
        runProjectionRepository.createRun(ownerB, "sess-b", "run-b",
                new CreateRunRequest("agent", "chat", Map.of("text", "hello b"), "ws", Map.of()));

        runEventStore.append(event("e-a", "tenant-a", "user-a", "sess-a", "run-a", 1, RunEventType.RUN_STARTED));
        runEventStore.append(event("e-b", "tenant-b", "user-b", "sess-b", "run-b", 1, RunEventType.RUN_STARTED));

        // listRecent: ownership-filtered, ordered by latest activity.
        PageResponse<SessionSummaryDto> recent = sessionRepository.listRecent(ownerA, 10, null);
        assertThat(recent.items()).extracting(SessionSummaryDto::sessionId)
                .containsExactly("sess-a")
                .doesNotContain("sess-b");

        // listRunsBySession: returns input maps for transcript USER messages.
        PageResponse<ConversationRunView> runs = runProjectionRepository.listRunsBySession(ownerA, "sess-a", 10, null);
        assertThat(runs.items()).hasSize(1);
        assertThat(runs.items().get(0).input()).containsEntry("text", "hello a");
        assertThat(runProjectionRepository.listRunsBySession(ownerA, "sess-b", 10, null).items()).isEmpty();

        // listBySessionRun: ownership-safe, excludes foreign events.
        List<RunEvent> aEvents = runEventStore.listBySessionRun(ownerA, "sess-a", "run-a", 0L, 10);
        assertThat(aEvents).extracting(RunEvent::eventId).containsExactly("e-a");
        assertThat(runEventStore.listBySessionRun(ownerB, "sess-a", "run-a", 0L, 10)).isEmpty();
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
