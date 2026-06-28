package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.controller.SessionController;
import io.github.pi_java.agent.adapter.web.security.PiPrincipal;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConversationControllerTest {

    @Test
    void recentEndpointReturnsTypedSessionSummariesAndThreadsContext() {
        FakeConversationQueryService conversation = new FakeConversationQueryService();
        SessionController controller = new SessionController(noopCommand(), noopSessionQuery(), conversation);

        PageResponse<SessionSummaryDto> response = controller.listRecentSessions(principal(), request(), 500, "cursor-1");

        assertThat(response.items()).extracting(SessionSummaryDto::sessionId).containsExactly("session-1");
        assertThat(conversation.lastTenant).isEqualTo("tenant-a");
        assertThat(conversation.lastUser).isEqualTo("user-a");
        assertThat(conversation.lastLimit).isEqualTo(100);
        assertThat(conversation.lastCursor).isEqualTo("cursor-1");
    }

    @Test
    void transcriptEndpointReturnsTypedTranscriptAndHistoryEndpointRemainsDiagnostic() {
        FakeConversationQueryService conversation = new FakeConversationQueryService();
        SessionController controller = new SessionController(noopCommand(), noopSessionQuery(), conversation);

        ConversationTranscriptResponse transcript = controller.getTranscript(principal(), request(), "session-1", 1000, null);
        SessionHistoryResponse history = controller.getSessionHistory(principal(), request(), "session-1");

        assertThat(transcript.sessionId()).isEqualTo("session-1");
        assertThat(transcript.messages()).isEmpty();
        assertThat(conversation.lastLimit).isEqualTo(500);
        assertThat(history.entries()).containsExactly(Map.of("diagnostic", true));
    }

    private static UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(new PiPrincipal("tenant-a", "user-a", Set.of("ROLE_USER")), "n/a");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("pi.traceId", "0123456789abcdef0123456789abcdef");
        request.setAttribute("pi.correlationId", "correlation-test");
        request.setAttribute("pi.causationId", "causation-test");
        return request;
    }

    private static SessionCommandService noopCommand() {
        return (RequestContext context, CreateSessionRequest request) -> new SessionResponse(context.tenantId(), context.userId(), "session-1", "ws", null, "ACTIVE", Instant.now(), Instant.now(), Map.of());
    }

    private static SessionQueryService noopSessionQuery() {
        return new SessionQueryService() {
            @Override
            public SessionResponse getSession(RequestContext context, String sessionId) {
                return new SessionResponse(context.tenantId(), context.userId(), sessionId, "ws", null, "ACTIVE", Instant.now(), Instant.now(), Map.of());
            }

            @Override
            public SessionHistoryResponse getSessionHistory(RequestContext context, String sessionId) {
                return new SessionHistoryResponse(getSession(context, sessionId), List.of(Map.of("diagnostic", true)));
            }
        };
    }

    private static final class FakeConversationQueryService implements ConversationQueryService {
        private String lastTenant;
        private String lastUser;
        private int lastLimit;
        private String lastCursor;

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
            capture(context, limit, cursor);
            return new PageResponse<>(List.of(new SessionSummaryDto("session-1", "Title", "ACTIVE", "preview", Instant.now(), Instant.now(), null, null, Map.of())), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
            capture(context, limit, cursor);
            return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
        }

        private void capture(RequestContext context, int limit, String cursor) {
            lastTenant = context.tenantId();
            lastUser = context.userId();
            lastLimit = limit;
            lastCursor = cursor;
        }
    }
}
