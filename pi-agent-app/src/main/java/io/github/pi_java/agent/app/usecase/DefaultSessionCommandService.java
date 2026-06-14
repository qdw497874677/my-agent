package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultSessionCommandService implements SessionCommandService {

    private final SessionRepository sessionRepository;
    private final AuditRepository auditRepository;
    private final Supplier<String> sessionIdSupplier;
    private final Clock clock;

    public DefaultSessionCommandService(
            SessionRepository sessionRepository,
            AuditRepository auditRepository,
            Supplier<String> sessionIdSupplier,
            Clock clock) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.sessionIdSupplier = Objects.requireNonNull(sessionIdSupplier, "sessionIdSupplier must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SessionResponse createSession(RequestContext context, CreateSessionRequest request) {
        String sessionId = sessionIdSupplier.get();
        SessionResponse response = sessionRepository.create(context, request, sessionId, clock.instant());
        auditRepository.record(context, "session.create", "session", sessionId, sessionId, null,
                Map.of("workspaceId", request.workspaceId()));
        return response;
    }
}
