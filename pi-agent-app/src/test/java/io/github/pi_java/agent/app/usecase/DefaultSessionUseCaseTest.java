package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-14T05:00:00Z");

    @Test
    void createSessionPersistsAndAudits() {
        RecordingSessionRepository sessions = new RecordingSessionRepository();
        RecordingAuditRepository audit = new RecordingAuditRepository();
        DefaultSessionCommandService service = new DefaultSessionCommandService(
                sessions, audit, () -> "session-1", Clock.fixed(NOW, ZoneOffset.UTC));

        SessionResponse response = service.createSession(context(), new CreateSessionRequest("workspace-1", Map.of("source", "test")));

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(sessions.createdSessionId).isEqualTo("session-1");
        assertThat(sessions.createdAt).isEqualTo(NOW);
        assertThat(audit.action).isEqualTo("session.create");
        assertThat(audit.resourceType).isEqualTo("session");
        assertThat(audit.resourceId).isEqualTo("session-1");
        assertThat(audit.sessionId).isEqualTo("session-1");
        assertThat(audit.details).containsEntry("workspaceId", "workspace-1");
    }

    @Test
    void getSessionDelegatesToRepository() {
        RecordingSessionRepository sessions = new RecordingSessionRepository();
        DefaultSessionQueryService service = new DefaultSessionQueryService(sessions);

        SessionResponse response = service.getSession(context(), "session-1");
        SessionHistoryResponse history = service.getSessionHistory(context(), "session-1");

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(sessions.findSessionId).isEqualTo("session-1");
        assertThat(history.session().sessionId()).isEqualTo("session-1");
        assertThat(sessions.historySessionId).isEqualTo("session-1");
    }

    private static RequestContext context() {
        return new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of()),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static SessionResponse session(String sessionId) {
        return new SessionResponse("tenant-1", "user-1", sessionId, "workspace-1", null, "ACTIVE", NOW, NOW, Map.of());
    }

    private static final class RecordingSessionRepository implements SessionRepository {
        String createdSessionId;
        Instant createdAt;
        String findSessionId;
        String historySessionId;

        @Override
        public SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now) {
            this.createdSessionId = sessionId;
            this.createdAt = now;
            return session(sessionId);
        }

        @Override
        public Optional<SessionResponse> findById(RequestContext context, String sessionId) {
            this.findSessionId = sessionId;
            return Optional.of(session(sessionId));
        }

        @Override
        public SessionHistoryResponse history(RequestContext context, String sessionId) {
            this.historySessionId = sessionId;
            return new SessionHistoryResponse(session(sessionId), List.of(Map.of("entryId", "entry-1")));
        }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        String action;
        String resourceType;
        String resourceId;
        String sessionId;
        Map<String, Object> details;

        @Override
        public void record(RequestContext context, String action, String resourceType, String resourceId,
                String sessionId, String runId, Map<String, Object> details) {
            this.action = action;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.sessionId = sessionId;
            this.details = details;
        }
    }
}
