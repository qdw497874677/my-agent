package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class DefaultSessionQueryService implements SessionQueryService {

    private final SessionRepository sessionRepository;

    public DefaultSessionQueryService(SessionRepository sessionRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
    }

    @Override
    public SessionResponse getSession(RequestContext context, String sessionId) {
        return sessionRepository.findById(context, sessionId)
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));
    }

    @Override
    public SessionHistoryResponse getSessionHistory(RequestContext context, String sessionId) {
        return sessionRepository.history(context, sessionId);
    }
}
