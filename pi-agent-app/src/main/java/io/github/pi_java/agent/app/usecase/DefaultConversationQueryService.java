package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.domain.event.RunEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Default {@link ConversationQueryService} that orchestrates ownership-safe
 * App repository ports and the {@link ConversationTranscriptAssembler} to
 * produce the Phase 16 typed conversation read model.
 *
 * <p>Old session lookup ({@link SessionRepository#findById}) is reused only for
 * ownership proof before transcript assembly; the diagnostic
 * {@code SessionHistoryResponse} path remains untouched for compatibility
 * (decision D-14). Transcript output is always expressed via Plan 01 typed
 * client DTOs, never raw map history (D-13).
 */
public final class DefaultConversationQueryService implements ConversationQueryService {

    private static final Set<String> ACTIVE_RUN_STATUSES = Set.of("QUEUED", "RUNNING", "SUSPENDED", "CANCELLING");

    private final SessionRepository sessionRepository;
    private final RunProjectionRepository runProjectionRepository;
    private final RunEventStore runEventStore;
    private final ConversationTranscriptAssembler assembler;

    public DefaultConversationQueryService(SessionRepository sessionRepository,
                                           RunProjectionRepository runProjectionRepository,
                                           RunEventStore runEventStore,
                                           ConversationTranscriptAssembler assembler) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.runProjectionRepository = Objects.requireNonNull(runProjectionRepository, "runProjectionRepository must not be null");
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
        this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
    }

    @Override
    public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
        return sessionRepository.listRecent(context, limit, cursor);
    }

    @Override
    public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
        // Ownership proof: refuse before loading any run/events (D-15).
        sessionRepository.findById(context, sessionId)
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));

        PageResponse<ConversationRunView> runsPage =
                runProjectionRepository.listRunsBySession(context, sessionId, limit, cursor);

        List<RunEvent> events = new ArrayList<>();
        for (ConversationRunView run : runsPage.items()) {
            events.addAll(runEventStore.listBySessionRun(context, sessionId, run.runId(), 0L, limit));
        }

        List<ConversationMessageDto> messages = assembler.assemble(sessionId, runsPage.items(), events);

        ActiveRun active = deriveActiveRun(runsPage.items());
        String nextCursor = runsPage.nextAfterSequence() != null
                ? String.valueOf(runsPage.nextAfterSequence())
                : null;

        return new ConversationTranscriptResponse(
                sessionId,
                messages,
                active.runId,
                active.runStatus,
                nextCursor,
                runsPage.hasMore(),
                java.util.Map.of());
    }

    private static ActiveRun deriveActiveRun(List<ConversationRunView> runs) {
        // The latest run wins so a freshly-created run surfaces as active even
        // when an earlier run is still suspending. Runs arrive in creation order
        // from the repository; iterate in reverse to prefer the newest.
        for (int i = runs.size() - 1; i >= 0; i--) {
            ConversationRunView run = runs.get(i);
            if (run.status() != null && ACTIVE_RUN_STATUSES.contains(run.status())) {
                return new ActiveRun(run.runId(), run.status());
            }
        }
        return new ActiveRun(null, null);
    }

    private record ActiveRun(String runId, String runStatus) {
    }
}
