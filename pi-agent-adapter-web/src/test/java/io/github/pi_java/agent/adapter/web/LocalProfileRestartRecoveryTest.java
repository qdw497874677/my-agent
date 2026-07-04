package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalDevStores;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunEventStore;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunProjectionRepository;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalSessionRepository;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.provider.SqliteLocalPersistence;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.app.usecase.ConversationTranscriptAssembler;
import io.github.pi_java.agent.app.usecase.DefaultConversationQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
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
 * Phase 20 same-DB local profile restart proof for SESS-05 and PROV-06.
 *
 * <p>These tests prove restart by discarding the first SQLite/store/config
 * objects, recreating fresh objects against the same DB file, and reading
 * through App-facing query/repository seams rather than Vaadin component state.
 */
class LocalProfileRestartRecoveryTest {

    @Test
    void providerConfigAndSelectedModelRecoverFromSameDatabaseAfterRestart(@TempDir Path tmp) {
        Path db = tmp.resolve("provider-restart.db");
        ProviderConfigStore firstStore = new ProviderConfigStore(db.toString());
        firstStore.update(new ProviderConfig(
                true,
                "https://provider.example/v1",
                "sk-local-secret",
                "gpt-4.1-mini",
                "openai-compatible",
                "/chat/completions"));

        ProviderConfigStore restartedStore = new ProviderConfigStore(db.toString());

        assertThat(restartedStore.isReady()).isTrue();
        assertThat(restartedStore.current().providerId()).isEqualTo("openai-compatible");
        assertThat(restartedStore.current().modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(restartedStore.current().baseUrl()).isEqualTo("https://provider.example/v1");
        assertThat(restartedStore.current().masked().apiKey()).doesNotContain("sk-local-secret");
    }

    @Test
    void recentSessionsTranscriptAndRunMetadataRecoverFromSameDatabaseAfterRestart(@TempDir Path tmp) {
        Path db = tmp.resolve("conversation-restart.db");
        RequestContext owner = context("tenant-a", "user-a");
        seedConversation(db, owner, "sess-a", "run-a", "What model handled this?", metadata(
                "openai-compatible:gpt-4.1-mini",
                "openai-compatible:gpt-4.1-mini",
                "openai-compatible",
                "gpt-4.1-mini",
                "NONE",
                "READY",
                null));

        RestartedLocalProfile restarted = restart(db);

        PageResponse<SessionSummaryDto> recent = restarted.query().listRecentSessions(owner, 10, null);
        assertThat(recent.items()).extracting(SessionSummaryDto::sessionId).containsExactly("sess-a");
        assertThat(recent.items().get(0).title()).isEqualTo("What model handled this?");

        ConversationTranscriptResponse transcript = restarted.query().getTranscript(owner, "sess-a", 10, null);
        assertThat(transcript.messages()).extracting(ConversationMessageDto::role)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);
        assertThat(transcript.messages()).extracting(ConversationMessageDto::text)
                .containsExactly("What model handled this?", "Handled by gpt-4.1-mini");
        assertThat(restarted.runs().findRun(owner, "sess-a", "run-a").orElseThrow().providerMetadata())
                .isEqualTo(new RunProviderMetadata(
                        "openai-compatible:gpt-4.1-mini",
                        "openai-compatible:gpt-4.1-mini",
                        "openai-compatible",
                        "gpt-4.1-mini",
                        "NONE",
                        "READY",
                        null));
    }

    @Test
    void fallbackMetadataSurvivesRestartForRestoredHistoryLabeling(@TempDir Path tmp) {
        Path db = tmp.resolve("fallback-restart.db");
        RequestContext owner = context("tenant-a", "user-a");
        seedConversation(db, owner, "sess-fallback", "run-fallback", "Can you answer locally?", metadata(
                "local-dev:not-configured",
                "local-dev:not-configured",
                "local-dev",
                "not-configured",
                "local",
                "NOT_CONFIGURED",
                "Local fallback was explicitly enabled"));

        RestartedLocalProfile restarted = restart(db);
        RunProviderMetadata restored = restarted.runs()
                .findRun(owner, "sess-fallback", "run-fallback")
                .orElseThrow()
                .providerMetadata();

        assertThat(restored.fallbackMode()).isEqualTo("local");
        assertThat(restored.readinessState()).isEqualTo("NOT_CONFIGURED");
        assertThat(restored.resolvedProviderId()).isEqualTo("local-dev");
        assertThat(restored.resolvedModelId()).isEqualTo("not-configured");
        assertThat(restored.safeErrorSummary()).contains("Local fallback");
        assertThat(restored.toString()).doesNotContain("apiKey", "Bearer", "sk-");
    }

    @Test
    void foreignTenantAndUserDataRemainExcludedAfterRestart(@TempDir Path tmp) {
        Path db = tmp.resolve("ownership-restart.db");
        RequestContext owner = context("tenant-a", "user-a");
        RequestContext foreign = context("tenant-b", "user-b");
        seedConversation(db, owner, "sess-owner", "run-owner", "owner prompt", metadata(
                "openai-compatible:gpt-owner", "openai-compatible:gpt-owner", "openai-compatible", "gpt-owner", "NONE", "READY", null));
        seedConversation(db, foreign, "sess-foreign", "run-foreign", "foreign prompt", metadata(
                "openai-compatible:gpt-foreign", "openai-compatible:gpt-foreign", "openai-compatible", "gpt-foreign", "NONE", "READY", null));

        RestartedLocalProfile restarted = restart(db);

        assertThat(restarted.query().listRecentSessions(owner, 10, null).items())
                .extracting(SessionSummaryDto::sessionId)
                .containsExactly("sess-owner")
                .doesNotContain("sess-foreign");
        assertThat(restarted.runs().listRunsBySession(owner, "sess-foreign", 10, null).items()).isEmpty();
        assertThat(restarted.events().listBySessionRun(owner, "sess-foreign", "run-foreign", 0L, 10)).isEmpty();
    }

    private static void seedConversation(Path db, RequestContext context, String sessionId, String runId,
                                         String userText, Map<String, Object> providerMetadata) {
        SqliteLocalPersistence persistence = new SqliteLocalPersistence(db.toString());
        LocalDevStores stores = new LocalDevStores(persistence);
        stores.loadAll();
        LocalSessionRepository sessions = new LocalSessionRepository(stores);
        LocalRunProjectionRepository runs = new LocalRunProjectionRepository(stores);
        LocalRunEventStore events = new LocalRunEventStore(stores);

        sessions.create(context, new CreateSessionRequest("workspace", Map.of("source", "restart-proof")),
                sessionId, Instant.parse("2026-07-04T00:00:00Z"));
        runs.createRun(context, sessionId, runId,
                new CreateRunRequest("general-agent", "chat", Map.of("text", userText), "workspace", providerMetadata));
        events.append(event("event-" + runId + "-delta", context, sessionId, runId, 1,
                RunEventType.MODEL_DELTA,
                new RunEventPayload.ModelDeltaPayload("openai-compatible:gpt-4.1-mini", "Handled by gpt-4.1-mini",
                        "openai-compatible", "gpt-4.1-mini", null, null, null)));
    }

    private static RestartedLocalProfile restart(Path db) {
        SqliteLocalPersistence restartedPersistence = new SqliteLocalPersistence(db.toString());
        LocalDevStores restartedStores = new LocalDevStores(restartedPersistence);
        restartedStores.loadAll();
        LocalSessionRepository sessions = new LocalSessionRepository(restartedStores);
        LocalRunProjectionRepository runs = new LocalRunProjectionRepository(restartedStores);
        LocalRunEventStore events = new LocalRunEventStore(restartedStores);
        ConversationQueryService query = new DefaultConversationQueryService(
                sessions, runs, events, new ConversationTranscriptAssembler());
        return new RestartedLocalProfile(query, runs, events);
    }

    private static RequestContext context(String tenantId, String userId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.of()),
                new CorrelationContext(trace(), "correlation-" + tenantId, "causation-" + tenantId));
    }

    private static RunEvent event(String eventId, RequestContext context, String sessionId, String runId,
                                  long sequence, RunEventType type, RunEventPayload payload) {
        return new RunEvent(
                eventId,
                new TenantId(context.tenantId()),
                new UserId(context.userId()),
                new SessionId(sessionId),
                new RunId(runId),
                new StepId("step-model"),
                new WorkspaceId("workspace"),
                sequence,
                Instant.parse("2026-07-04T00:00:00Z").plusSeconds(sequence),
                type,
                new TraceId(trace()),
                new CorrelationId(context.correlationId()),
                new CausationId("causation-" + context.tenantId()),
                payload,
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "none"));
    }

    private static Map<String, Object> metadata(String requestedModelRef, String selectedModelRef,
                                                String providerId, String modelId, String fallbackMode,
                                                String readinessState, String safeErrorSummary) {
        return Map.of(
                "requestedModelRef", requestedModelRef,
                "selectedModelRef", selectedModelRef,
                "resolvedProviderId", providerId,
                "resolvedModelId", modelId,
                "fallbackMode", fallbackMode,
                "readinessState", readinessState,
                "safeErrorSummary", safeErrorSummary == null ? "" : safeErrorSummary);
    }

    private static String trace() {
        return "0123456789abcdef0123456789abcdef";
    }

    private record RestartedLocalProfile(ConversationQueryService query,
                                         LocalRunProjectionRepository runs,
                                         LocalRunEventStore events) {
    }
}
