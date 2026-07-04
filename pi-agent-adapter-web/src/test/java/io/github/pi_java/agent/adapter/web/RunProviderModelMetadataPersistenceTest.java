package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalDevStores;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunProjectionRepository;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalSessionRepository;
import io.github.pi_java.agent.adapter.web.provider.SqliteLocalPersistence;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 20 Plan 03 local-profile proof for safe provider/model/fallback
 * metadata persistence. Mirrors the cloud JDBC run projection contract while
 * preserving Phase 16 ownership-filtered local read-model behavior.
 */
class RunProviderModelMetadataPersistenceTest {

    @Test
    void sqliteExistingDatabasesUpgradeInPlaceWithProviderMetadataColumn(@TempDir Path tmp) throws Exception {
        Path db = tmp.resolve("upgrade.db");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE local_runs (run_id TEXT PRIMARY KEY, session_id TEXT NOT NULL, tenant_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, workspace_id TEXT NOT NULL, status TEXT NOT NULL, created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL, result_json TEXT, failure_json TEXT)");
            stmt.execute("INSERT INTO local_runs VALUES ('run-old','sess-a','tenant-a','user-a','ws','QUEUED'," +
                    "'2026-07-04T00:00:00Z','2026-07-04T00:00:00Z',NULL,NULL)");
        }

        SqliteLocalPersistence persistence = new SqliteLocalPersistence(db.toString());
        persistence.saveRun("run-new", "sess-a", "tenant-a", "user-a", "ws", "QUEUED",
                "2026-07-04T00:00:01Z", "2026-07-04T00:00:01Z", null, null,
                "{\"text\":\"hi\"}", "trace", "correlation",
                "{\"resolvedProviderId\":\"openai\",\"resolvedModelId\":\"gpt-4o-mini\"}");

        List<Map<String, String>> runs = persistence.loadRunsBySession("tenant-a", "user-a", "sess-a", 10);
        assertThat(runs).extracting(row -> row.get("run_id")).contains("run-old", "run-new");
        assertThat(runs.stream().filter(row -> "run-new".equals(row.get("run_id"))).findFirst().orElseThrow())
                .containsEntry("provider_metadata_json", "{\"resolvedProviderId\":\"openai\",\"resolvedModelId\":\"gpt-4o-mini\"}");
    }

    @Test
    void sqliteSaveAndLoadPreservesSafeMetadataAcrossPersistenceInstances(@TempDir Path tmp) {
        Path db = tmp.resolve("metadata.db");
        SqliteLocalPersistence first = new SqliteLocalPersistence(db.toString());
        first.saveRun("run-a", "sess-a", "tenant-a", "user-a", "ws", "QUEUED",
                "2026-07-04T00:00:00Z", "2026-07-04T00:00:00Z", null, null,
                "{\"text\":\"hello\"}", "trace", "correlation",
                "{\"requestedModelRef\":\"local:gpt\",\"selectedModelRef\":\"openai:gpt\",\"resolvedProviderId\":\"openai\",\"resolvedModelId\":\"gpt\",\"fallbackMode\":\"NONE\",\"readinessState\":\"READY\",\"safeErrorSummary\":\"ok\"}");

        SqliteLocalPersistence second = new SqliteLocalPersistence(db.toString());
        List<Map<String, String>> loaded = second.loadRunsBySession("tenant-a", "user-a", "sess-a", 10);

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).get("provider_metadata_json"))
                .contains("requestedModelRef", "resolvedProviderId", "fallbackMode")
                .doesNotContain("apiKey", "Bearer");
    }

    @Test
    void localDevStoresHydrateMetadataIntoRunAndConversationViews(@TempDir Path tmp) {
        Path db = tmp.resolve("stores.db");
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(db.toString());
        LocalDevStores stores = new LocalDevStores(persistence);
        LocalSessionRepository sessions = new LocalSessionRepository(stores);
        LocalRunProjectionRepository runs = new LocalRunProjectionRepository(stores);
        RequestContext owner = context("tenant-a", "user-a");

        sessions.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a", Instant.parse("2026-07-04T00:00:00Z"));
        runs.createRun(owner, "sess-a", "run-a", new CreateRunRequest("agent", "chat", Map.of("text", "hello"), "ws", Map.of(
                "requestedModelRef", "local:gpt",
                "selectedModelRef", "openai:gpt",
                "resolvedProviderId", "openai",
                "resolvedModelId", "gpt",
                "fallbackMode", "LOCAL_PROFILE_FALLBACK",
                "readinessState", "READY",
                "safeErrorSummary", "401 Bearer secret-token apiKey=sk-live-secret",
                "apiKey", "sk-live-secret")));

        LocalDevStores reloaded = new LocalDevStores(new SqliteLocalPersistence(db.toString()));
        reloaded.loadAll();
        LocalRunProjectionRepository reloadedRuns = new LocalRunProjectionRepository(reloaded);

        RunResponse found = reloadedRuns.findRun(owner, "sess-a", "run-a").orElseThrow();
        assertThat(found.providerMetadata()).isEqualTo(new RunProviderMetadata(
                "local:gpt", "openai:gpt", "openai", "gpt", "LOCAL_PROFILE_FALLBACK", "READY",
                "401 Bearer [REDACTED] apiKey=[REDACTED]"));
        PageResponse<ConversationRunView> listed = reloadedRuns.listRunsBySession(owner, "sess-a", 10, null);
        assertThat(listed.items()).hasSize(1);
        assertThat(listed.items().get(0).providerMetadata()).isEqualTo(found.providerMetadata());
        assertThat(reloadedRuns.listRunsBySession(context("tenant-b", "user-b"), "sess-a", 10, null).items()).isEmpty();
    }

    private static RequestContext context(String tenantId, String userId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.of()),
                new CorrelationContext("0123456789abcdef0123456789abcdef", "correlation-" + tenantId, "causation-" + tenantId));
    }
}
