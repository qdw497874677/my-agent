package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;

public interface SessionQueryService {

    SessionResponse getSession(RequestContext context, String sessionId);

    SessionHistoryResponse getSessionHistory(RequestContext context, String sessionId);
}
