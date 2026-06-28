package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;

/**
 * App read-model boundary for the Phase 16 conversation read model
 * (requirements SESS-01, SESS-04; decisions D-01, D-13).
 *
 * <p>This is a dedicated Conversation boundary rather than an overload of
 * {@code SessionQueryService}: recent-session summaries and typed transcripts
 * carry their own product semantics (latest-activity ordering, role/status
 * enums, redaction/visibility, message ordering) that must not leak back into
 * the diagnostic {@code SessionHistoryResponse} path (decision D-14).
 *
 * <p>All methods require an ownership-carrying {@link RequestContext}; downstream
 * repository implementations enforce tenant/user/session/run filters (D-15).
 * Output is restricted to Plan 01 typed client DTOs.
 */
public interface ConversationQueryService {

    /**
     * Lists recent sessions for the caller as typed
     * {@link SessionSummaryDto} projections (SESS-01, D-09..D-12).
     *
     * @param context ownership/tracing context; never {@code null}.
     * @param limit   maximum number of summaries to return.
     * @param cursor  opaque pagination cursor; {@code null} starts from the most recent activity.
     * @return a page of recent-session summaries ordered by latest activity, never {@code null}.
     */
    PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor);

    /**
     * Assembles the typed conversation transcript for a session (SESS-04,
     * D-05..D-08, D-13, D-16).
     *
     * @param context   ownership/tracing context; never {@code null}.
     * @param sessionId the session whose transcript is requested.
     * @param limit     maximum number of runs/messages to fold per page.
     * @param cursor    opaque pagination cursor; {@code null} starts from the oldest run.
     * @return a typed transcript response, never {@code null}.
     */
    ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor);
}
