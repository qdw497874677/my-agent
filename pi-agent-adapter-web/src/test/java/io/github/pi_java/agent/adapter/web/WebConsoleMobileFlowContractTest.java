package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import io.github.pi_java.agent.adapter.web.ui.console.AgentCatalogPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.SessionListPanel;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebConsoleMobileFlowContractTest {

    @Test
    void consoleExposesSegmentedSwitcherForRouteLocalPanels() {
        ConsoleView view = new ConsoleView();

        Div switcher = onlyChildWithAttribute(view, "data-role", "console-panel-switcher");
        assertThat(switcher.getClassNames()).contains("pi-console-panel-switcher");

        List<Button> controls = switcher.getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();
        assertThat(controls).extracting(Button::getText).containsExactly("Chat", "Agents", "Sessions", "Run");
        assertThat(controls).extracting(button -> button.getElement().getAttribute("data-action"))
                .containsOnly("show-console-panel");
        assertThat(controls).extracting(button -> button.getElement().getAttribute("data-console-target"))
                .containsExactly("chat", "agents", "sessions", "run-context");
    }

    @Test
    void chatPanelIsActiveByDefaultAndDesktopColumnContractRemains() {
        ConsoleView view = new ConsoleView();

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertPanelState(view, "chat", "true");
        assertPanelState(view, "agents", "false");
        assertPanelState(view, "sessions", "false");
        assertPanelState(view, "run-context", "false");
        assertThat(view.getElement().getAttribute("data-layout")).isEqualTo("three-column-workbench");
        assertThat(view.columnOrder()).containsExactly("sessions", "chat-event-stream", "run-context");
    }

    @Test
    void switchingPanelsPreservesExistingChatPanelAndMessages() {
        ConsoleView view = new ConsoleView();
        ChatEventStreamPanel originalChatPanel = view.chatPanel();

        view.planChatSubmission("Keep this message while browsing sessions");
        view.showConsolePanel("sessions");

        assertThat(view.activeConsolePanel()).isEqualTo("sessions");
        assertThat(view.chatPanel()).isSameAs(originalChatPanel);
        assertThat(view.chatPanel().messageCount()).isEqualTo(1);
        assertPanelState(view, "chat", "false");
        assertPanelState(view, "sessions", "true");
    }

    @Test
    void agentCatalogCardsExposeGeneralAgentPrimaryActionContract() {
        AgentCatalogPanel panel = new AgentCatalogPanel();

        panel.showCatalog(new AgentCatalogResponse(List.of(generalAgent("cloud-general-agent", "start-chat"))));

        assertThat(panel.getClassNames()).contains("pi-agent-catalog-panel");
        assertThat(panel.cardsElement().getAttribute("data-role")).isEqualTo("agent-catalog-cards");
        assertThat(panel.renderedCards().getFirst().getClassNames()).contains("pi-agent-card");
        assertThat(panel.renderedCards().getFirst().getElement().getAttribute("data-agent-id"))
                .isEqualTo("cloud-general-agent");
        assertThat(panel.renderedCards().getFirst().getElement().getAttribute("data-general-agent")).isEqualTo("true");
        assertThat(panel.renderedCards().getFirst().primaryActionMarker()).isEqualTo("general-agent-start");
        assertThat(panel.renderedCards().getFirst().firstEntryAction()).isEqualTo("start-chat");
    }

    @Test
    void sessionHistoryRendersCardMetadataAndActiveIdentity() {
        SessionListPanel panel = new SessionListPanel();

        panel.showSession("session-a", "Architecture review", Instant.parse("2026-06-23T05:00:00Z"));
        panel.selectSession("session-a");

        Div card = panel.sessionCards().getFirst();
        assertThat(panel.listElement().getAttribute("data-role")).isEqualTo("session-list");
        assertThat(card.getClassNames()).contains("pi-session-card");
        assertThat(card.getElement().getAttribute("data-role")).isEqualTo("session-card");
        assertThat(card.getElement().getAttribute("data-session-id")).isEqualTo("session-a");
        assertThat(card.getElement().getAttribute("data-session-active")).isEqualTo("true");
        assertThat(fieldText(card, "session-title")).contains("Selected session");
        assertThat(fieldText(card, "session-status")).contains("ready");
        assertThat(fieldText(card, "session-updated-at")).contains("not yet updated");
    }

    @Test
    void selectingPriorSessionMarksActiveCardAndReturnsToChatPanel() {
        ConsoleView view = new ConsoleView();

        view.showConsolePanel("sessions");
        view.selectSession("session-history");

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-history");
        assertThat(view.sessionListPanel().sessionCards().getFirst().getElement().getAttribute("data-session-active"))
                .isEqualTo("true");
    }

    private static void assertPanelState(ConsoleView view, String panel, String active) {
        Div wrapper = onlyChildWithAttribute(view, "data-console-panel", panel);
        assertThat(wrapper.getElement().getAttribute("data-console-panel-active")).isEqualTo(active);
    }

    private static Div onlyChildWithAttribute(ConsoleView view, String attribute, String value) {
        List<Div> matches = view.getChildren()
                .filter(Div.class::isInstance)
                .map(Div.class::cast)
                .filter(child -> value.equals(child.getElement().getAttribute(attribute)))
                .toList();
        assertThat(matches).extracting(Component::getElement).hasSize(1);
        return matches.getFirst();
    }

    private static String fieldText(Div card, String field) {
        return card.getChildren()
                .filter(component -> field.equals(component.getElement().getAttribute("data-field")))
                .findFirst()
                .map(Component::getElement)
                .map(element -> element.getTextRecursively())
                .orElse("");
    }

    private static AgentCatalogItemDto generalAgent(String id, String actionId) {
        return new AgentCatalogItemDto(
                id,
                "Cloud General Agent",
                "General purpose agent for mobile console testing",
                Set.of("chat"),
                Set.of("streaming"),
                new AgentCatalogItemDto.ModelRefDto("openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini"),
                Set.of("builtin.info"),
                Set.of("tool:read"),
                Set.of("LOW"),
                Set.of("READ_ONLY"),
                List.of(new AgentCatalogItemDto.EntryActionDto(actionId, "Start chat", "CREATE_RUN", "CHAT", Map.of())),
                Duration.ofSeconds(30),
                Map.of("source", "test"));
    }
}
