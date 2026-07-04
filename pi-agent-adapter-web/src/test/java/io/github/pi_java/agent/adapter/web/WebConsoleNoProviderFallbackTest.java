package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigController;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebConsoleNoProviderFallbackTest {

    @TempDir
    Path tempDir;

    @Test
    void disabledProviderBlocksSendBeforeSessionRunAndUserMessage() {
        ProviderConfigStore store = storeWith(new ProviderConfig(false, "https://example.invalid/v1", "", "gpt-4.1-mini", "openai-compatible", "/chat/completions"));
        CountingBridge bridge = new CountingBridge();
        ConsoleView view = consoleView(store, bridge, false);

        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("hello without key");

        assertThat(plan).isNull();
        assertThat(bridge.createSessionCalls).isZero();
        assertThat(bridge.createRunCalls).isZero();
        assertThat(view.chatPanel().messageCount()).isZero();
    }

    @Test
    void blankApiKeyBlocksSendWithActionableProviderMessage() {
        ProviderConfigStore store = storeWith(new ProviderConfig(true, "https://example.invalid/v1", "", "gpt-4.1-mini", "openai-compatible", "/chat/completions"));
        ConsoleView view = consoleView(store, new CountingBridge(), false);

        view.planChatSubmission("hello blank key");

        Element status = onlyDescendantWithAttribute(view, "data-role", "provider-status").getElement();
        Element blocked = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement();
        assertThat(status.getAttribute("data-provider-ready")).isEqualTo("false");
        assertThat(blocked.getAttribute("data-refresh-state")).isEqualTo("blocked");
        assertThat(blocked.getTextRecursively()).containsIgnoringCase("provider").containsIgnoringCase("api key");
    }

    @Test
    void explicitLocalFallbackAllowsSendAndLabelsModelAreaAndAssistantBubble() {
        ProviderConfigStore store = storeWith(new ProviderConfig(false, "https://example.invalid/v1", "", "gpt-4.1-mini", "openai-compatible", "/chat/completions"));
        CountingBridge bridge = new CountingBridge();
        ConsoleView view = consoleView(store, bridge, true);

        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("hello local fallback");

        assertThat(plan).isNotNull();
        assertThat(bridge.createSessionCalls).isEqualTo(1);
        assertThat(bridge.createRunCalls).isEqualTo(1);
        assertThat(descendants(view).stream()
                .filter(component -> "local".equals(component.getElement().getAttribute("data-fallback-mode"))))
                .isNotEmpty();
        Element label = onlyDescendantWithAttribute(view.chatPanel(), "data-role", "fallback-label").getElement();
        assertThat(label.getTextRecursively()).containsIgnoringCase("local fallback");
    }

    private ConsoleView consoleView(ProviderConfigStore store, CountingBridge bridge, boolean localFallback) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()),
                bridge, new RunEventRenderer(), store, new ProviderConfigController(store), localFallback);
    }

    private ProviderConfigStore storeWith(ProviderConfig config) {
        ProviderConfigStore store = new ProviderConfigStore(tempDir.resolve("fallback-" + System.nanoTime() + ".db").toString());
        store.update(config);
        return store;
    }

    private static Component onlyDescendantWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = descendants(root).stream()
                .filter(component -> value.equals(component.getElement().getAttribute(attribute)))
                .toList();
        assertThat(matches).as(attribute + "=" + value).hasSize(1);
        return matches.getFirst();
    }

    private static List<Component> descendants(Component root) {
        List<Component> found = new ArrayList<>();
        collect(root, found);
        return found;
    }

    private static void collect(Component component, List<Component> found) {
        found.add(component);
        component.getChildren().forEach(child -> collect(child, found));
    }

    private static final class CountingBridge implements ConsoleRunExecutionBridge {
        int createSessionCalls;
        int createRunCalls;

        @Override
        public SessionResponse createSession() {
            createSessionCalls++;
            return new SessionResponse("tenant", "user", "session-fallback", "workspace", null, "ACTIVE", Instant.now(), Instant.now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            createRunCalls++;
            return new RunResponse("tenant", "user", sessionId, "run-fallback", "workspace", "QUEUED", "trace", "correlation", Instant.now(), Instant.now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            RunEventDto event = new RunEventDto("event-1", "tenant", "user", sessionId, runId, null, "workspace", 1,
                    Instant.now(), "model.delta", "trace", "correlation", null, "USER", null, "model.delta", 1,
                    Map.of("text", "local fallback reply"));
            return new EventHistoryResponse(sessionId, runId, List.of(event), afterSequence, 1, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "cancelled", true, Instant.now(), "trace", "correlation");
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
        }
    }
}
