package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import java.util.List;
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
}
