package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {

    SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now);

    Optional<SessionResponse> findById(RequestContext context, String sessionId);

    SessionHistoryResponse history(RequestContext context, String sessionId);
}
