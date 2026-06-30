package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleStreamingBubbleLifecycleTest {

    @Test
    void beginAssistantMessageCreatesOnePendingPrimaryAssistantBubbleWithStableSelectors() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        Component assistant = assistants.getFirst();
        assertThat(assistant.getElement().getAttribute("data-message-role")).isEqualTo("assistant");
        assertThat(assistant.getElement().getAttribute("data-message-kind")).isEqualTo("primary-bubble");
        assertThat(assistant.getElement().getAttribute("data-session-id")).isEqualTo("session-1");
        assertThat(assistant.getElement().getAttribute("data-run-id")).isEqualTo("run-1");
        assertThat(assistant.getElement().getAttribute("data-step-id")).isEqualTo("step-1");
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("pending");
        assertThat(assistant.getElement().getAttribute("data-message-status")).isEqualTo("pending");
        assertThat(assistant.getElement().getAttribute("data-stream-aggregation-key")).isEqualTo("session-1::run-1::step-1");
        assertThat(assistant.getElement().getTextRecursively().toLowerCase()).contains("pending");
    }

    @Test
    void assistantDeltasAppendToSameBubbleWithoutIncreasingPrimaryAssistantCount() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");
        panel.appendAssistantDelta("session-1", "run-1", "step-1", "Hello");
        panel.appendAssistantDelta("session-1", "run-1", "step-1", " world");

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getTextRecursively()).isEqualTo("Hello world");
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("streaming");
        assertThat(panel.messages()).containsExactly("Hello world");
    }

    @Test
    void terminalStatesMutateExistingAssistantBubbleWithoutBlankMessages() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");
        panel.markAssistantTerminal("session-1", "run-1", "step-1", ConversationMessageStatus.COMPLETED, null);

        assertThat(primaryAssistantBubbles(panel)).hasSize(1);
        Component completed = primaryAssistantBubbles(panel).getFirst();
        assertThat(completed.getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(completed.getElement().getAttribute("data-message-status")).isEqualTo("completed");
        assertThat(panel.messages()).isEmpty();

        ChatEventStreamPanel failedPanel = new ChatEventStreamPanel();
        failedPanel.beginAssistantMessage("session-1", "run-failed", "step-1");
        failedPanel.appendAssistantDelta("session-1", "run-failed", "step-1", "Partial text");
        failedPanel.markAssistantTerminal("session-1", "run-failed", "step-1", ConversationMessageStatus.FAILED, "Provider unavailable");
        Component failed = primaryAssistantBubbles(failedPanel).getFirst();
        assertThat(failed.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(failed.getElement().getTextRecursively()).contains("Partial text", "Provider unavailable");
        assertThat(primaryAssistantBubbles(failedPanel)).hasSize(1);

        ChatEventStreamPanel cancelledPanel = new ChatEventStreamPanel();
        cancelledPanel.beginAssistantMessage("session-1", "run-cancelled", "step-1");
        cancelledPanel.markAssistantTerminal("session-1", "run-cancelled", "step-1", ConversationMessageStatus.CANCELLED, null);
        assertThat(primaryAssistantBubbles(cancelledPanel).getFirst().getElement().getTextRecursively().toLowerCase())
                .contains("cancelled");

        ChatEventStreamPanel partialPanel = new ChatEventStreamPanel();
        partialPanel.beginAssistantMessage("session-1", "run-partial", "step-1");
        partialPanel.markAssistantTerminal("session-1", "run-partial", "step-1", ConversationMessageStatus.PARTIAL, null);
        assertThat(primaryAssistantBubbles(partialPanel).getFirst().getElement().getTextRecursively().toLowerCase())
                .contains("partial");
    }

    private static List<Component> primaryAssistantBubbles(ChatEventStreamPanel panel) {
        return descendantsWithAttribute(panel, "data-message-role").stream()
                .filter(component -> "assistant".equals(component.getElement().getAttribute("data-message-role")))
                .filter(component -> "primary-bubble".equals(component.getElement().getAttribute("data-message-kind")))
                .toList();
    }

    private static List<Component> descendantsWithAttribute(Component root, String attribute) {
        return descendants(root.getElement())
                .filter(element -> element.hasAttribute(attribute))
                .flatMap(element -> element.getComponent().stream())
                .toList();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }
}
