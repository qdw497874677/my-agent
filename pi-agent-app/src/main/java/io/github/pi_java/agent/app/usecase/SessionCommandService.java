package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;

public interface SessionCommandService {

    SessionResponse createSession(RequestContext context, CreateSessionRequest request);
}
