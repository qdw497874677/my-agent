package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleConversationReadModelHookTest {

    @Test
    void consoleHttpClientExposesTypedConversationPaths() {
        ConsoleHttpClient client = new ConsoleHttpClient();

        assertThat(client.recentSessionsPath(20, "next")).isEqualTo("/api/sessions/recent?limit=20&cursor=next");
        assertThat(client.sessionTranscriptPath("session-1", 100, null)).isEqualTo("/api/sessions/session-1/transcript?limit=100");
        assertThat(client.sessionTranscriptResponseType()).isEqualTo(ConversationTranscriptResponse.class);
    }

    @Test
    void consoleLoadsRecentAndTranscriptThroughTypedBridge() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()), bridge, new RunEventRenderer());

        view.loadRecentSessionsForProof();
        ConsoleView.SessionSelectionPlan plan = view.selectSession("session-typed");

        assertThat(bridge.recentCalled).isTrue();
        assertThat(bridge.transcriptSessionId).isEqualTo("session-typed");
        assertThat(view.sessionListPanel().recentSessionIds()).contains("session-typed");
        assertThat(view.chatPanel().messages()).containsExactly("typed hello", "typed answer");
        assertThat(plan.historyPath()).isEqualTo("/api/sessions/session-typed/history");
    }

    private static final class RecordingBridge implements ConsoleRunExecutionBridge {
        private boolean recentCalled;
        private String transcriptSessionId;

        @Override
        public SessionResponse createSession() {
            return new SessionResponse("tenant", "user", "session-typed", "ws", null, "ACTIVE", now(), now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            return new RunResponse("tenant", "user", sessionId, "run-1", "ws", "QUEUED", "trace", "correlation", now(), now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            return new EventHistoryResponse(sessionId, runId, List.of(), afterSequence, afterSequence, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "CANCELLED", true, now(), "trace", "correlation");
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            recentCalled = true;
            return new PageResponse<>(List.of(new SessionSummaryDto("session-typed", "Typed", "ACTIVE", "typed hello", now(), now(), null, null, Map.of())), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            transcriptSessionId = sessionId;
            return new ConversationTranscriptResponse(sessionId, List.of(
                    message(sessionId, "m1", ConversationMessageRole.USER, "typed hello"),
                    message(sessionId, "m2", ConversationMessageRole.ASSISTANT, "typed answer")
            ), null, null, null, false, Map.of());
        }

        private static ConversationMessageDto message(String sessionId, String id, ConversationMessageRole role, String text) {
            return new ConversationMessageDto(id, sessionId, "run-1", null, role, text, ConversationMessageStatus.COMPLETED, now(), now(), 1L, 1L, Map.of(), true, false);
        }
    }

    private static Instant now() {
        return Instant.parse("2026-06-01T00:00:00Z");
    }
}
