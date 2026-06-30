package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.ConversationEventReducer;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleStreamingCancellationTest {

    @Test
    void cancelRunningRunCallsBridgeMarksBubbleCancelledAndPreservesPartialText() {
        RecordingBridge bridge = new RecordingBridge();
        bridge.events.add(event("delta-1", "session-cancel", "run-cancel", 1, "model.delta", Map.of("text", "partial answer")));
        ConsoleView view = consoleView(bridge);

        view.planChatSubmission("hello");
        ConsoleView.CancelPlan plan = view.planCancelRunningRun("user stopped response");

        assertThat(plan.request().reason()).isEqualTo("user stopped response");
        assertThat(bridge.cancelCalls).isEqualTo(1);
        assertThat(bridge.cancelSessionId).isEqualTo("session-cancel");
        assertThat(bridge.cancelRunId).isEqualTo("run-cancel");
        assertThat(bridge.cancelReason).isEqualTo("user stopped response");
        Component assistant = primaryAssistantBubbles(view.chatPanel()).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isIn("cancelled", "partial");
        assertThat(assistant.getElement().getTextRecursively()).contains("partial answer");
        assertThat(assistant.getElement().getTextRecursively().toLowerCase()).containsAnyOf("cancelled", "partial", "stopped");
    }

    @Test
    void reducerIgnoresLateDeltaAfterLocalCancellationStop() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.reduce(event("delta-1", "session-cancel", "run-cancel", 1, "model.delta", Map.of("text", "partial"))), panel, renderer);
        ConversationEventReducer.apply(reducer.stopRun("session-cancel", "run-cancel", "step-1", "user stopped response"), panel, renderer);
        ConversationEventReducer.Operation late = reducer.reduce(event("delta-2", "session-cancel", "run-cancel", 99, "model.delta", Map.of("text", " late")));
        ConversationEventReducer.apply(late, panel, renderer);

        assertThat(late.kind()).isEqualTo(ConversationEventReducer.Operation.Kind.IGNORE);
        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getTextRecursively()).contains("partial").doesNotContain("late");
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("cancelled");
    }

    @Test
    void cancelIsNotUiOnlyWhenRunIsActive() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = consoleView(bridge);

        view.planChatSubmission("hello");
        view.planCancelRunningRun("mobile user requested cancellation");

        assertThat(bridge.cancelCalls).isEqualTo(1);
        assertThat(bridge.cancelSessionId).isEqualTo("session-cancel");
        assertThat(bridge.cancelRunId).isEqualTo("run-cancel");
        assertThat(bridge.cancelReason).isEqualTo("mobile user requested cancellation");
    }

    @Test
    void modelErrorEventMarksAssistantFailedWithSafeStatusCard() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.reduce(event("delta-1", "session-fail", "run-fail", 1, "model.delta", Map.of("text", "partial"))), panel, renderer);
        ConversationEventReducer.apply(reducer.reduce(event("error-1", "session-fail", "run-fail", 2, "model.error",
                Map.of("errorCategory", "provider_unavailable", "apiKey", "sk-secret", "token", "raw-token"))), panel, renderer);

        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(assistant.getElement().getTextRecursively()).contains("partial", "provider_unavailable");
        assertThat(statusChips(assistant)).contains(ConversationMessageStatus.FAILED.wireValue());
        assertThat(assistant.getElement().getTextRecursively()).doesNotContain("apiKey", "sk-secret", "token", "raw-token", "{", "}");
    }

    @Test
    void rawExceptionBodiesAndSecretPayloadFieldsAreNotRenderedAsAssistantProse() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.reduce(event("delta-1", "session-fail", "run-fail", 1, "model.delta", Map.of("text", "partial"))), panel, renderer);
        ConversationEventReducer.apply(reducer.reduce(event("failed-1", "session-fail", "run-fail", 2, "run.failed",
                Map.of("error", "java.lang.RuntimeException: upstream body {apiKey=sk-secret, token=raw-token, secret=value}"))), panel, renderer);

        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(assistant.getElement().getTextRecursively()).contains("partial");
        assertThat(assistant.getElement().getTextRecursively()).doesNotContain("RuntimeException", "apiKey", "sk-secret", "token", "raw-token", "secret=value", "{");
    }

    @Test
    void terminalTransitionsKeepBufferedTextVisibleBeforeTerminalStatus() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.reduce(event("delta-1", "session-done", "run-done", 1, "model.delta", Map.of("text", "final text"))), panel, renderer);
        ConversationEventReducer.apply(reducer.reduce(event("done-1", "session-done", "run-done", 2, "run.completed", Map.of())), panel, renderer);

        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(assistant.getElement().getTextRecursively()).contains("final text");
        assertThat(panel.messages()).containsExactly("final text");
    }

    private static ConsoleView consoleView(RecordingBridge bridge) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), new DefaultAgentCatalogQueryService(), bridge, new RunEventRenderer());
    }

    private static List<Component> primaryAssistantBubbles(ChatEventStreamPanel panel) {
        return descendants(panel.getElement())
                .filter(element -> "assistant".equals(element.getAttribute("data-message-role")))
                .filter(element -> "primary-bubble".equals(element.getAttribute("data-message-kind")))
                .flatMap(element -> element.getComponent().stream())
                .toList();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static List<String> statusChips(Component root) {
        return descendants(root.getElement())
                .filter(element -> element.hasAttribute("data-status-chip"))
                .map(element -> element.getAttribute("data-status-chip"))
                .toList();
    }

    private static RunEventDto event(String eventId, String sessionId, String runId, long sequence, String type, Map<String, Object> payload) {
        return new RunEventDto(eventId, "tenant-1", "user-1", sessionId, runId, "step-1", "workspace-1",
                sequence, Instant.parse("2026-06-01T00:00:00Z"), type, "trace-1", "correlation-1", null,
                "USER", null, "schema", 1, payload);
    }

    private static final class RecordingBridge implements ConsoleRunExecutionBridge {
        private final List<RunEventDto> events = new ArrayList<>();
        private int cancelCalls;
        private String cancelSessionId;
        private String cancelRunId;
        private String cancelReason;

        @Override
        public SessionResponse createSession() {
            return new SessionResponse("tenant-1", "user-1", "session-cancel", "workspace-1", null, "ACTIVE", Instant.now(), Instant.now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            return new RunResponse("tenant-1", "user-1", sessionId, "run-cancel", "workspace-1", "RUNNING", "trace-1", "correlation-1", Instant.now(), Instant.now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            List<RunEventDto> filtered = events.stream().filter(event -> event.sequence() > afterSequence).toList();
            long next = filtered.stream().mapToLong(RunEventDto::sequence).max().orElse(afterSequence);
            return new EventHistoryResponse(sessionId, runId, filtered, afterSequence, next, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            cancelCalls++;
            cancelSessionId = sessionId;
            cancelRunId = runId;
            cancelReason = request.reason();
            return new RunStatusResponse(sessionId, runId, "cancelled", true, Instant.now(), "trace-1", "correlation-1");
        }

        @Override
        public PageResponse listRecentSessions(int limit, String cursor) {
            return new PageResponse(List.of(), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
        }
    }
}
