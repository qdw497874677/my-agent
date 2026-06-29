package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.dom.Element;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebConsoleSessionRestoreUxTest {

    @Test
    void newConsoleStartsWithNewConversationBanner() {
        ConsoleView view = new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()), new RecordingBridge(), new RunEventRenderer());

        Component banner = onlyDescendantWithAttribute(view, "data-role", "active-session-banner");

        assertThat(banner.getElement().getAttribute("data-active-session-state")).isEqualTo("new");
        assertThat(banner.getElement().getTextRecursively()).contains("New conversation");
    }

    @Test
    void selectingHistoricalSessionUpdatesBannerToStableTitle() {
        ConsoleView view = viewWith(new RecordingBridge());
        view.loadRecentSessionsForProof();

        view.selectSession("session-old");

        Component banner = onlyDescendantWithAttribute(view, "data-role", "active-session-banner");
        assertThat(banner.getElement().getAttribute("data-active-session-state")).isEqualTo("continued");
        assertThat(banner.getElement().getTextRecursively()).contains("Continue: Stable Title");
    }

    @Test
    void newConversationActionClearsContinuationAndReturnsBannerToNewState() {
        ConsoleView view = viewWith(new RecordingBridge());
        view.loadRecentSessionsForProof();
        view.selectSession("session-old");

        Button action = (Button) onlyDescendantWithAttribute(view, "data-action", "new-conversation");
        action.click();

        Component banner = onlyDescendantWithAttribute(view, "data-role", "active-session-banner");
        assertThat(banner.getElement().getAttribute("data-active-session-state")).isEqualTo("new");
        assertThat(banner.getElement().getTextRecursively()).contains("New conversation");
        assertThat(view.sessionListPanel().selectedSessionId()).isNull();
        assertThat(view.chatPanel().messages()).isEmpty();
        assertThat(view.activeConsolePanel()).isEqualTo("chat");
    }

    @Test
    void selectingHistoricalSessionLoadsTypedTranscriptAndClearsPreviousFeed() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = viewWith(bridge);
        view.planChatSubmission("temporary current feed");

        view.selectSession("session-old");

        assertThat(bridge.transcriptSessionId).isEqualTo("session-old");
        assertThat(view.chatPanel().messages()).containsExactly("old question", "old answer");
        assertThat(view.chatPanel().messages()).doesNotContain("temporary current feed");
        assertThat(descendants(view.chatPanel().getElement())
                .filter(element -> "user".equals(element.getAttribute("data-message-role")))
                .map(Element::getTextRecursively))
                .containsExactly("old question");
    }

    @Test
    void selectingHistoricalSessionHighlightsCardReturnsToChatAndRestoresActiveCursor() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = viewWith(bridge);
        view.loadRecentSessionsForProof();
        view.showConsolePanel("sessions");

        view.selectSession("session-old");
        int appended = view.refreshActiveRunEvents();

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-old");
        assertThat(view.sessionListPanel().sessionCards().getFirst().getElement().getAttribute("data-session-active"))
                .isEqualTo("true");
        assertThat(bridge.eventAfterSequences).containsExactly(42L);
        assertThat(appended).isZero();
    }

    private static ConsoleView viewWith(RecordingBridge bridge) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()), bridge, new RunEventRenderer());
    }

    private static Component onlyDescendantWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = descendants(root.getElement())
                .filter(element -> value.equals(element.getAttribute(attribute)))
                .map(element -> element.getComponent().orElseThrow())
                .toList();
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static final class RecordingBridge implements ConsoleRunExecutionBridge {
        private final List<String> createSessionCalls = new ArrayList<>();
        private final List<String> createRunSessions = new ArrayList<>();
        private final List<Long> eventAfterSequences = new ArrayList<>();
        private String transcriptSessionId;

        @Override
        public SessionResponse createSession() {
            createSessionCalls.add("createSession");
            return new SessionResponse("tenant", "user", "session-new", "ws", null, "ACTIVE", now(), now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            createRunSessions.add(sessionId);
            return new RunResponse("tenant", "user", sessionId, "run-created", "ws", "QUEUED", "trace", "correlation", now(), now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            eventAfterSequences.add(afterSequence);
            return new EventHistoryResponse(sessionId, runId, List.of(), afterSequence, afterSequence, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "CANCELLED", true, now(), "trace", "correlation");
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            return new PageResponse<>(List.of(
                    new SessionSummaryDto("session-old", "Stable Title", "ACTIVE", "older preview", now(), now(), "run-old", "RUNNING", Map.of())
            ), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            transcriptSessionId = sessionId;
            return new ConversationTranscriptResponse(sessionId, List.of(
                    message(sessionId, "m1", ConversationMessageRole.USER, "old question"),
                    message(sessionId, "m2", ConversationMessageRole.ASSISTANT, "old answer")
            ), "run-old", "RUNNING", "42", false, Map.of());
        }

        private static ConversationMessageDto message(String sessionId, String id, ConversationMessageRole role, String text) {
            return new ConversationMessageDto(id, sessionId, "run-old", null, role, text, ConversationMessageStatus.COMPLETED, now(), now(), 1L, 1L, Map.of(), true, false);
        }
    }

    private static Instant now() {
        return Instant.parse("2026-06-01T00:00:00Z");
    }
}
