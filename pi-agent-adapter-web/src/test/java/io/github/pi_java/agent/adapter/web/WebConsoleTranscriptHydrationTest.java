package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleTranscriptHydrationTest {

    @Test
    void replaceTranscriptClearsOldFeedAndRestoresUserAssistantBubblesInOrder() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        panel.appendUserMessage("old in-memory message");

        panel.replaceTranscript(List.of(
                message("m1", ConversationMessageRole.USER, "hello", ConversationMessageStatus.COMPLETED),
                message("m2", ConversationMessageRole.ASSISTANT, "hi there", ConversationMessageStatus.COMPLETED)
        ));

        List<Component> restored = descendantsWithAttribute(panel, "data-message-role");
        assertThat(restored).hasSize(2);
        assertThat(restored).extracting(component -> component.getElement().getAttribute("data-message-role"))
                .containsExactly("user", "assistant");
        assertThat(restored).extracting(component -> component.getElement().getTextRecursively())
                .containsExactly("hello", "hi there");
        assertThat(panel.messages()).containsExactly("hello", "hi there");
    }

    @Test
    void userAndAssistantBubblesExposePrimaryAlignmentClasses() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(
                message("m1", ConversationMessageRole.USER, "right side", ConversationMessageStatus.COMPLETED),
                message("m2", ConversationMessageRole.ASSISTANT, "left side", ConversationMessageStatus.COMPLETED)
        ));

        Component user = onlyMessage(panel, "user");
        Component assistant = onlyMessage(panel, "assistant");
        assertThat(user.getElement().getAttribute("data-message-kind")).isEqualTo("primary-bubble");
        assertThat(user.getElement().getAttribute("data-bubble-align")).isEqualTo("right");
        assertThat(user.getClassNames()).contains("pi-transcript-message", "pi-transcript-user");
        assertThat(assistant.getElement().getAttribute("data-message-kind")).isEqualTo("primary-bubble");
        assertThat(assistant.getElement().getAttribute("data-bubble-align")).isEqualTo("left");
        assertThat(assistant.getClassNames()).contains("pi-transcript-message", "pi-transcript-assistant");
    }

    @Test
    void restoredMessagesExposeSessionRunStatusAndStreamStateSelectors() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(
                new ConversationMessageDto("m1", "session-1", "run-1", "step-1", ConversationMessageRole.USER,
                        "with metadata", ConversationMessageStatus.PARTIAL, now(), now(), 1L, 1L, Map.of(), true, false)
        ));

        Component message = onlyMessage(panel, "user");
        assertThat(message.getElement().getAttribute("data-session-id")).isEqualTo("session-1");
        assertThat(message.getElement().getAttribute("data-run-id")).isEqualTo("run-1");
        assertThat(message.getElement().getAttribute("data-message-status")).isEqualTo("partial");
        assertThat(message.getElement().getAttribute("data-stream-state")).isEqualTo("partial");
    }

    @Test
    void emptyTranscriptRestoresEmptyStateAndClearsMessagesAndComponents() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        panel.replaceTranscript(List.of(message("m1", ConversationMessageRole.USER, "existing", ConversationMessageStatus.COMPLETED)));

        panel.replaceTranscript(List.of());

        assertThat(panel.messages()).isEmpty();
        assertThat(panel.componentCount()).isZero();
        assertThat(descendantsWithAttribute(panel, "data-message-role")).isEmpty();
        assertThat(descendantsWithAttribute(panel, "data-state")).hasSize(1);
        assertThat(descendantsWithAttribute(panel, "data-state").getFirst().getElement().getAttribute("data-state"))
                .isEqualTo("empty");
    }

    @Test
    void toolMessagesRenderAsCompactSecondaryCardsInsteadOfAssistantProse() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(message("tool-1", ConversationMessageRole.TOOL, "Tool returned safe summary", ConversationMessageStatus.COMPLETED)));

        Component tool = onlyMessage(panel, "tool");
        assertThat(tool.getElement().getAttribute("data-message-kind")).isEqualTo("secondary-card");
        assertThat(tool.getElement().getAttribute("data-transcript-card")).isEqualTo("tool");
        assertThat(tool.getElement().getAttribute("data-bubble-align")).isNull();
        assertThat(tool.getClassNames()).contains("pi-transcript-card", "pi-transcript-tool");
        assertThat(tool.getElement().getTextRecursively()).contains("Tool returned safe summary");
        assertThat(panel.componentCount()).isEqualTo(1);
    }

    @Test
    void errorAndFailedMessagesRenderAsCompactStatusCardsWithVisibleAbnormalStatus() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(message("error-1", ConversationMessageRole.ERROR, "Provider failed", ConversationMessageStatus.FAILED)));

        Component error = onlyMessage(panel, "error");
        assertThat(error.getElement().getAttribute("data-message-kind")).isEqualTo("secondary-card");
        assertThat(error.getElement().getAttribute("data-transcript-card")).isEqualTo("error");
        assertThat(error.getElement().getAttribute("data-message-status")).isEqualTo("failed");
        assertThat(error.getElement().getTextRecursively()).contains("failed", "Provider failed");
    }

    @Test
    void completedCardsStayQuietWhilePartialCancelledAndFailedStatusesAreVisible() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(
                message("tool-1", ConversationMessageRole.TOOL, "quiet tool", ConversationMessageStatus.COMPLETED),
                message("tool-2", ConversationMessageRole.TOOL, "partial tool", ConversationMessageStatus.PARTIAL),
                message("error-1", ConversationMessageRole.ERROR, "cancelled tool", ConversationMessageStatus.CANCELLED)
        ));

        List<Component> cards = descendantsWithAttribute(panel, "data-message-role");
        assertThat(cards.get(0).getElement().getTextRecursively()).doesNotContain("completed");
        assertThat(cards.get(1).getElement().getTextRecursively()).contains("partial");
        assertThat(cards.get(2).getElement().getTextRecursively()).contains("cancelled");
    }

    @Test
    void redactedMetadataIsNotDumpedAsRawJsonInTranscriptCards() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.replaceTranscript(List.of(new ConversationMessageDto("tool-1", "session-1", "run-1", null,
                ConversationMessageRole.TOOL, "safe summary", ConversationMessageStatus.FAILED, now(), now(), 1L, 1L,
                Map.of("secret", "should-not-render", "nested", Map.of("token", "raw-json")), true, true)));

        String rendered = onlyMessage(panel, "tool").getElement().getTextRecursively();
        assertThat(rendered).contains("safe summary", "failed");
        assertThat(rendered).doesNotContain("should-not-render", "raw-json", "{", "}", "secret", "token");
    }

    private static ConversationMessageDto message(String id, ConversationMessageRole role, String text, ConversationMessageStatus status) {
        return new ConversationMessageDto(id, "session-1", "run-1", null, role, text, status, now(), now(), 1L, 1L, Map.of(), true, false);
    }

    private static Component onlyMessage(ChatEventStreamPanel panel, String role) {
        List<Component> matches = descendantsWithAttribute(panel, "data-message-role").stream()
                .filter(component -> role.equals(component.getElement().getAttribute("data-message-role")))
                .toList();
        assertThat(matches).hasSize(1);
        return matches.getFirst();
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

    private static Instant now() {
        return Instant.parse("2026-06-01T00:00:00Z");
    }
}
