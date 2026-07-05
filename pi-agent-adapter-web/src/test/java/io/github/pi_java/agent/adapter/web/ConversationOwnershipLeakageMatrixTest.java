package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalDevStores;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunEventStore;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalRunProjectionRepository;
import io.github.pi_java.agent.adapter.web.config.LocalDevRuntimeBeanConfiguration.LocalSessionRepository;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 21 VER-02 local ownership matrix for the conversation read model.
 *
 * <p>This is the always-runnable SQLite substitute for the cloud JDBC ownership
 * proof. It exercises {@link DefaultConversationQueryService} through the same
 * local App repository ports used by the local profile and seeds one allowed
 * tenant/user/session/run plus negative rows for a foreignTenant, foreignUser,
 * foreignSession, and foreignRun. Both recent-session and transcript reads must
 * remain scoped to the caller's tenant, user, session, and run boundaries.
 */
class ConversationOwnershipLeakageMatrixTest {

    @Test
    void foreignTenantForeignUserForeignSessionAndForeignRunAreExcludedFromRecentSessionsAndTranscript(@TempDir Path tmp) {
        LocalConversationHarness harness = harness(tmp.resolve("ownership-matrix.db"));

        RequestContext owner = context("tenant-owner", "user-owner");
        RequestContext foreignTenant = context("tenant-foreign", "user-owner");
        RequestContext foreignUser = context("tenant-owner", "user-foreign");

        harness.seed(owner, "session-allowed", "run-allowed", "allowed prompt", "allowed answer", 1);
        harness.seed(foreignTenant, "session-foreign-tenant", "run-foreign-tenant",
                "foreignTenant prompt", "foreignTenant answer", 2);
        harness.seed(foreignUser, "session-foreign-user", "run-foreign-user",
                "foreignUser prompt", "foreignUser answer", 3);
        harness.seed(owner, "session-foreign-session", "run-foreign-session",
                "foreignSession prompt", "foreignSession answer", 4);
        harness.runs().createRun(owner, "session-allowed", "run-foreign-run",
                new CreateRunRequest("general-agent", "chat",
                        Map.of("text", "foreignRun prompt"), "workspace", Map.of()));
        harness.events().append(event("event-foreign-run-delta", owner, "session-allowed", "run-foreign-run", 5,
                new RunEventPayload.ModelDeltaPayload("local-dev:model", "foreignRun answer",
                        "local-dev", "model", null, null, null)));

        PageResponse<SessionSummaryDto> recent = harness.query().listRecentSessions(owner, 20, null);
        assertThat(recent.items())
                .extracting(SessionSummaryDto::sessionId)
                .contains("session-allowed", "session-foreign-session")
                .doesNotContain("session-foreign-tenant", "session-foreign-user");
        assertThat(recent.items())
                .extracting(SessionSummaryDto::title)
                .contains("allowed prompt", "foreignSession prompt")
                .doesNotContain("foreignTenant prompt", "foreignUser prompt");

        ConversationTranscriptResponse transcript = harness.query().getTranscript(owner, "session-allowed", 20, null);

        assertThat(transcript.sessionId()).isEqualTo("session-allowed");
        assertThat(transcript.messages()).extracting(ConversationMessageDto::role)
                .containsExactly(
                        ConversationMessageRole.USER,
                        ConversationMessageRole.ASSISTANT,
                        ConversationMessageRole.USER,
                        ConversationMessageRole.ASSISTANT);
        assertThat(transcript.messages()).extracting(ConversationMessageDto::text)
                .containsExactly("allowed prompt", "allowed answer", "foreignRun prompt", "foreignRun answer")
                .doesNotContain("foreignTenant prompt", "foreignTenant answer",
                        "foreignUser prompt", "foreignUser answer",
                        "foreignSession prompt", "foreignSession answer");
        assertThat(transcript.messages()).extracting(ConversationMessageDto::sessionId)
                .containsOnly("session-allowed");
        assertThat(transcript.messages()).extracting(ConversationMessageDto::runId)
                .contains("run-allowed", "run-foreign-run")
                .doesNotContain("run-foreign-tenant", "run-foreign-user", "run-foreign-session");
    }

    @Test
    void foreignRunEventWithSameRunIdButDifferentTenantUserSessionCannotLeakIntoTranscript(@TempDir Path tmp) {
        LocalConversationHarness harness = harness(tmp.resolve("colliding-run.db"));

        RequestContext owner = context("tenant-owner", "user-owner");
        RequestContext foreignTenant = context("tenant-foreign", "user-owner");
        RequestContext foreignUser = context("tenant-owner", "user-foreign");

        harness.sessions().create(owner, new CreateSessionRequest("workspace", Map.of()),
                "session-allowed", Instant.parse("2026-07-05T00:00:00Z"));
        harness.sessions().create(foreignTenant, new CreateSessionRequest("workspace", Map.of()),
                "session-foreign-tenant", Instant.parse("2026-07-05T00:00:01Z"));
        harness.sessions().create(foreignUser, new CreateSessionRequest("workspace", Map.of()),
                "session-foreign-user", Instant.parse("2026-07-05T00:00:02Z"));

        harness.runs().createRun(owner, "session-allowed", "run-collide",
                new CreateRunRequest("general-agent", "chat", Map.of("text", "allowed prompt"), "workspace", Map.of()));
        harness.events().append(event("event-allowed-delta", owner, "session-allowed", "run-collide", 1,
                new RunEventPayload.ModelDeltaPayload("local-dev:model", "allowed answer",
                        "local-dev", "model", null, null, null)));
        harness.events().append(event("event-foreignTenant-delta", foreignTenant, "session-foreign-tenant", "run-collide", 2,
                new RunEventPayload.ModelDeltaPayload("local-dev:model", "foreignTenant answer",
                        "local-dev", "model", null, null, null)));
        harness.events().append(event("event-foreignUser-delta", foreignUser, "session-foreign-user", "run-collide", 3,
                new RunEventPayload.ModelDeltaPayload("local-dev:model", "foreignUser answer",
                        "local-dev", "model", null, null, null)));
        harness.events().append(event("event-foreignSession-delta", owner, "session-foreign-session", "run-collide", 4,
                new RunEventPayload.ModelDeltaPayload("local-dev:model", "foreignSession answer",
                        "local-dev", "model", null, null, null)));

        ConversationTranscriptResponse transcript = harness.query().getTranscript(owner, "session-allowed", 20, null);

        assertThat(transcript.messages()).extracting(ConversationMessageDto::text)
                .containsExactly("allowed prompt", "allowed answer")
                .doesNotContain("foreignTenant answer", "foreignUser answer", "foreignSession answer");
        assertThat(transcript.messages()).extracting(ConversationMessageDto::runId)
                .containsOnly("run-collide");
        assertThat(transcript.messages()).extracting(ConversationMessageDto::sessionId)
                .containsOnly("session-allowed");
    }

    private static LocalConversationHarness harness(Path db) {
        LocalDevStores stores = new LocalDevStores(new SqliteLocalPersistence(db.toString()));
        stores.loadAll();
        LocalSessionRepository sessions = new LocalSessionRepository(stores);
        LocalRunProjectionRepository runs = new LocalRunProjectionRepository(stores);
        LocalRunEventStore events = new LocalRunEventStore(stores);
        ConversationQueryService query = new DefaultConversationQueryService(
                sessions, runs, events, new ConversationTranscriptAssembler());
        return new LocalConversationHarness(query, sessions, runs, events);
    }

    private static RequestContext context(String tenantId, String userId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.of()),
                new CorrelationContext(trace(), "correlation-" + tenantId + "-" + userId,
                        "causation-" + tenantId + "-" + userId));
    }

    private static RunEvent event(String eventId, RequestContext context, String sessionId, String runId,
                                  long sequence, RunEventPayload payload) {
        return new RunEvent(
                eventId,
                new TenantId(context.tenantId()),
                new UserId(context.userId()),
                new SessionId(sessionId),
                new RunId(runId),
                new StepId("step-model"),
                new WorkspaceId("workspace"),
                sequence,
                Instant.parse("2026-07-05T00:00:00Z").plusSeconds(sequence),
                RunEventType.MODEL_DELTA,
                new TraceId(trace()),
                new CorrelationId(context.correlationId()),
                new CausationId("causation-" + context.tenantId()),
                payload,
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "none"));
    }

    private static String trace() {
        return "0123456789abcdef0123456789abcdef";
    }

    private record LocalConversationHarness(ConversationQueryService query,
                                            LocalSessionRepository sessions,
                                            LocalRunProjectionRepository runs,
                                            LocalRunEventStore events) {

        void seed(RequestContext context, String sessionId, String runId, String prompt,
                  String answer, long sequence) {
            sessions.create(context, new CreateSessionRequest("workspace", Map.of()),
                    sessionId, Instant.parse("2026-07-05T00:00:00Z").plusSeconds(sequence));
            runs.createRun(context, sessionId, runId,
                    new CreateRunRequest("general-agent", "chat", Map.of("text", prompt), "workspace", Map.of()));
            events.append(event("event-" + runId + "-delta", context, sessionId, runId, sequence,
                    new RunEventPayload.ModelDeltaPayload("local-dev:model", answer,
                            "local-dev", "model", null, null, null)));
        }
    }
}
