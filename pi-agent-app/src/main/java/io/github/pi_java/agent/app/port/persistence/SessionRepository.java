package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {

    SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now);

    Optional<SessionResponse> findById(RequestContext context, String sessionId);

    SessionHistoryResponse history(RequestContext context, String sessionId);

    /**
     * Lists recent sessions for the caller defined by {@link RequestContext},
     * ordered by latest conversation activity (decision D-09) and summarized as
     * typed {@link SessionSummaryDto} projections for the conversation read
     * model (requirement SESS-01).
     *
     * <p>Infrastructure implementations must enforce tenant/user ownership
     * filters at the query/SQL layer (decision D-15). {@code cursor} is an
     * opaque, implementation-defined pagination cursor; {@code null} starts
     * from the most recent activity.
     *
     * <p>This method is a default that throws until Phase 16 plan 03 provides
     * the ownership-aware JDBC implementation; it is declared as a default so
     * existing test fakes and infrastructure stubs continue to compile.
     *
     * @return a page of recent-session summaries, never {@code null}.
     */
    default PageResponse<SessionSummaryDto> listRecent(RequestContext context, int limit, String cursor) {
        throw new UnsupportedOperationException(
                "listRecent not yet implemented; Phase 16 plan 03 provides the ownership-aware implementation");
    }
}
