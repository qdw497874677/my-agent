package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigController;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.ConversationEventReducer;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Phase21ConversationRegressionGateTest {

    @TempDir
    Path tempDir;

    @Test
    void noKeyFallbackBlocksSendBeforeSessionRunOrUserBubble() {
        ProviderConfigStore store = storeWith(new ProviderConfig(true, "https://example.invalid/v1", "", "gpt-4.1-mini", "openai-compatible", "/chat/completions"));
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = consoleView(store, bridge, false);

        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("hello without configured key");

        assertThat(plan).isNull();
        assertThat(bridge.createSessionCalls).isEmpty();
        assertThat(bridge.createRunSessions).isEmpty();
        assertThat(view.chatPanel().messageCount()).isZero();
        Element providerStatus = onlyDescendantWithAttribute(view, "data-role", "provider-status").getElement();
        Element blockedStatus = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement();
        assertThat(providerStatus.getAttribute("data-provider-ready")).isEqualTo("false");
        assertThat(blockedStatus.getAttribute("data-refresh-state")).isEqualTo("blocked");
        assertThat(blockedStatus.getTextRecursively()).containsIgnoringCase("provider").containsIgnoringCase("api key");
    }

    @Test
    void configuredProviderPathUsesSelectedProviderModelSnapshotWithoutNetworkCredentials() {
        ProviderConfigStore store = storeWith(new ProviderConfig(true, "https://example.invalid/v1", "sk-fake-ready", "gpt-ready", "fake-provider", "/chat/completions"));
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = consoleView(store, bridge, false);

        modelSelector(view).setValue("phase21-selected-model");
        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("prove configured provider path");

        assertThat(plan).isNotNull();
        assertThat(bridge.lastRequest).isNotNull();
        assertThat(bridge.lastRequest.metadata())
                .containsEntry("selectedModelRef", "fake-provider:phase21-selected-model")
                .containsEntry("resolvedProviderId", "fake-provider")
                .containsEntry("resolvedModelId", "phase21-selected-model")
                .containsEntry("fallbackMode", "NONE")
                .containsEntry("readinessState", "READY");
        assertThat(bridge.createRunSessions).containsExactly("session-new");
    }

    @Test
    void recentSessionRestoreHydratesPriorUserAndAssistantTurns() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = defaultConsoleView(bridge);
        view.loadRecentSessionsForProof();

        view.selectSession("session-old");

        assertThat(bridge.transcriptSessionId).isEqualTo("session-old");
        assertThat(view.chatPanel().messages()).containsExactly("old question", "old answer");
        assertThat(descendants(view.chatPanel().getElement())
                .filter(element -> "user".equals(element.getAttribute("data-message-role")))
                .map(Element::getTextRecursively))
                .containsExactly("old question");
        assertThat(descendants(view.chatPanel().getElement())
                .filter(element -> "assistant".equals(element.getAttribute("data-message-role")))
                .map(Element::getTextRecursively))
                .anySatisfy(text -> assertThat(text).contains("old answer"));
    }

    @Test
    void sameSessionContinuationKeepsRestoredSessionIdForFollowUpRun() {
        RecordingBridge bridge = new RecordingBridge();
        ConsoleView view = defaultConsoleView(bridge);
        view.loadRecentSessionsForProof();
        view.selectSession("session-old");

        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("follow up on restored session");

        assertThat(plan.createSessionPath()).isNull();
        assertThat(plan.sessionId()).isEqualTo("session-old");
        assertThat(bridge.createSessionCalls).isEmpty();
        assertThat(bridge.createRunSessions).containsExactly("session-old");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-old");
    }

    @Test
    void streamingCoalescingKeepsMultipleDeltasInOneAssistantBubbleForSameRun() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        apply(reducer.reduce(event("delta-1", "session-stream", "run-stream", 1, "model.delta", Map.of("text", "Hello"))), panel, renderer);
        apply(reducer.reduce(event("delta-2", "session-stream", "run-stream", 2, "model.delta", Map.of("textDelta", " phase"))), panel, renderer);
        apply(reducer.reduce(event("delta-3", "session-stream", "run-stream", 3, "model.delta", Map.of("delta", " 21"))), panel, renderer);
        apply(reducer.reduce(event("done-1", "session-stream", "run-stream", 4, "run.completed", Map.of())), panel, renderer);

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getAttribute("data-run-id")).isEqualTo("run-stream");
        assertThat(assistants.getFirst().getElement().getTextRecursively()).isEqualTo("Hello phase 21");
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(panel.messages()).containsExactly("Hello phase 21");
    }

    @Test
    void cancellationAndErrorStatesStopLateDeltasAndRenderSafeTerminalBubbles() {
        ConversationEventReducer cancelReducer = new ConversationEventReducer();
        ChatEventStreamPanel cancelPanel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        apply(cancelReducer.reduce(event("delta-1", "session-cancel", "run-cancel", 1, "model.delta", Map.of("text", "partial"))), cancelPanel, renderer);
        apply(cancelReducer.stopRun("session-cancel", "run-cancel", "step-1", "user stopped response"), cancelPanel, renderer);
        ConversationEventReducer.Operation late = cancelReducer.reduce(event("delta-2", "session-cancel", "run-cancel", 2, "model.delta", Map.of("text", " late")));
        apply(late, cancelPanel, renderer);

        Component cancelled = primaryAssistantBubbles(cancelPanel).getFirst();
        assertThat(late.kind()).isEqualTo(ConversationEventReducer.Operation.Kind.IGNORE);
        assertThat(cancelled.getElement().getAttribute("data-stream-state")).isEqualTo("cancelled");
        assertThat(cancelled.getElement().getTextRecursively()).contains("partial", "user stopped response").doesNotContain("late");
        assertThat(cancelPanel.messages()).containsExactly("partial");

        ConversationEventReducer errorReducer = new ConversationEventReducer();
        ChatEventStreamPanel errorPanel = new ChatEventStreamPanel();
        apply(errorReducer.reduce(event("delta-err", "session-error", "run-error", 1, "model.delta", Map.of("text", "safe partial"))), errorPanel, renderer);
        apply(errorReducer.reduce(event("failed-err", "session-error", "run-error", 2, "run.failed",
                Map.of("message", "Provider unavailable", "apiKey", "sk-hidden", "authorization", "Bearer hidden"))), errorPanel, renderer);

        Component failed = primaryAssistantBubbles(errorPanel).getFirst();
        assertThat(failed.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(failed.getElement().getTextRecursively()).contains("safe partial", "Provider unavailable");
        assertThat(failed.getElement().getTextRecursively()).doesNotContain("apiKey", "sk-hidden", "Bearer", "authorization", "{");
    }

    @Test
    void providerErrorsAreActionableAndDoNotExposeSecretLookingValues() throws Exception {
        String secret = "sk-phase21-secret";
        try (ModelServer server = ModelServer.responding(503, "Bearer " + secret + " raw upstream outage", "Bearer " + secret)) {
            ProviderConfigStore store = storeWith(new ProviderConfig(true, server.baseUrl(), secret, "gpt-error", "fake-provider", "/chat/completions"));
            ProviderConfigController.ModelListResponse response = new ProviderConfigController(store).listModels();

            assertThat(response.state()).isEqualTo("error");
            assertThat(response.ready()).isTrue();
            assertThat(response.message()).containsIgnoringCase("provider");
            assertThat(response.error()).containsIgnoringCase("service unavailable");
            assertThat(response.message()).doesNotContain(secret).doesNotContain("Bearer").doesNotContain("apiKey");
            assertThat(response.error()).doesNotContain(secret).doesNotContain("Bearer").doesNotContain("apiKey");
        }
    }

    private ConsoleView consoleView(ProviderConfigStore store, RecordingBridge bridge, boolean localFallback) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()),
                bridge, new RunEventRenderer(), store, new ProviderConfigController(store), localFallback);
    }

    private static ConsoleView defaultConsoleView(RecordingBridge bridge) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()), bridge, new RunEventRenderer());
    }

    private ProviderConfigStore storeWith(ProviderConfig config) {
        ProviderConfigStore store = new ProviderConfigStore(tempDir.resolve("phase21-" + System.nanoTime() + ".db").toString());
        store.update(config);
        return store;
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> modelSelector(ConsoleView view) {
        return (ComboBox<String>) onlyDescendantWithAttribute(view, "data-role", "model-selector");
    }

    private static Component onlyDescendantWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = descendants(root.getElement())
                .filter(element -> value.equals(element.getAttribute(attribute)))
                .map(element -> element.getComponent().orElseThrow())
                .toList();
        assertThat(matches).as(attribute + "=" + value).hasSize(1);
        return matches.getFirst();
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

    private static void apply(ConversationEventReducer.Operation operation, ChatEventStreamPanel panel, RunEventRenderer renderer) {
        ConversationEventReducer.apply(operation, panel, renderer);
    }

    private static RunEventDto event(String eventId, String sessionId, String runId, long sequence, String type, Map<String, Object> payload) {
        return new RunEventDto(eventId, "tenant-1", "user-1", sessionId, runId, "step-1", "workspace-1",
                sequence, Instant.parse("2026-06-01T00:00:00Z"), type, "trace-1", "correlation-1", null,
                "USER", null, "schema", 1, payload);
    }

    private static final class RecordingBridge implements ConsoleRunExecutionBridge {
        private final List<String> createSessionCalls = new ArrayList<>();
        private final List<String> createRunSessions = new ArrayList<>();
        private CreateRunRequest lastRequest;
        private String transcriptSessionId;

        @Override
        public SessionResponse createSession() {
            createSessionCalls.add("createSession");
            return new SessionResponse("tenant", "user", "session-new", "workspace", null, "ACTIVE", now(), now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            lastRequest = request;
            createRunSessions.add(sessionId);
            return new RunResponse("tenant", "user", sessionId, "run-created", "workspace", "RUNNING", "trace", "correlation", now(), now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            return new EventHistoryResponse(sessionId, runId, List.of(), afterSequence, afterSequence, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "cancelled", true, now(), "trace", "correlation");
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            return new PageResponse<>(List.of(new SessionSummaryDto("session-old", "Stable Title", "ACTIVE", "older preview", now(), now(), "run-old", "RUNNING", Map.of())),
                    limit, null, null, false);
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

    private static final class ModelServer implements AutoCloseable {
        private final HttpServer server;

        private ModelServer(HttpServer server) {
            this.server = server;
        }

        static ModelServer responding(int status, String body, String authHeaderEcho) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/models", exchange -> {
                if (authHeaderEcho != null) {
                    exchange.getResponseHeaders().add("X-Auth-Echo", authHeaderEcho);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return new ModelServer(server);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
