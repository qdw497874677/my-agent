package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import io.github.pi_java.agent.adapter.web.ui.console.SessionListPanel;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WebConsoleSessionListPanelTest {

    @Test
    void showRecentSessionsRendersBoundedCardsInProvidedOrder() {
        SessionListPanel panel = new SessionListPanel();

        panel.showRecentSessions(List.of(summary("session-a", "Alpha"), summary("session-b", "Beta")), "session-b", true);

        assertThat(panel.sessionCards()).hasSize(2);
        assertThat(panel.recentSessionIds()).containsExactly("session-a", "session-b");
        assertThat(panel.sessionCards()).extracting(card -> card.getElement().getAttribute("data-session-id"))
                .containsExactly("session-a", "session-b");
        assertThat(descendantsWithAttribute(panel.listElement(), "data-role", "session-more")).hasSize(1);
    }

    @Test
    void recentSessionCardsExposeStableCompactFieldsAndSelection() {
        SessionListPanel panel = new SessionListPanel();

        panel.showRecentSessions(List.of(summary("session-a", "Alpha")), "session-a", false);

        Div card = panel.sessionCards().getFirst();
        assertThat(card.getElement().getAttribute("data-role")).isEqualTo("session-card");
        assertThat(card.getElement().getAttribute("data-session-id")).isEqualTo("session-a");
        assertThat(card.getElement().getAttribute("data-session-active")).isEqualTo("true");
        assertThat(fieldText(card, "session-title")).isEqualTo("Alpha");
        assertThat(fieldText(card, "session-preview")).isEqualTo("Preview for Alpha");
        assertThat(fieldText(card, "session-updated-at")).isEqualTo("2026-06-23T05:00:00Z");
        assertThat(fieldText(card, "session-status")).isEqualTo("RUNNING");
    }

    @Test
    void activeRunStatusOverridesSessionStatusAndProviderMetadataIsNotRendered() {
        SessionListPanel panel = new SessionListPanel();

        panel.showRecentSessions(List.of(
                new SessionSummaryDto(
                        "session-active",
                        "Active run",
                        "ready",
                        "Latest answer",
                        Instant.parse("2026-06-23T05:00:00Z"),
                        Instant.parse("2026-06-22T05:00:00Z"),
                        "run-1",
                        "STREAMING",
                        Map.of("provider", "openai", "model", "gpt-x")),
                new SessionSummaryDto(
                        "session-idle",
                        "Idle",
                        "ready",
                        "Idle preview",
                        Instant.parse("2026-06-23T06:00:00Z"),
                        Instant.parse("2026-06-22T06:00:00Z"),
                        null,
                        null,
                        Map.of("provider", "anthropic", "model", "claude"))),
                null,
                false);

        assertThat(fieldText(panel.sessionCards().get(0), "session-status")).isEqualTo("STREAMING");
        assertThat(fieldText(panel.sessionCards().get(1), "session-status")).isEqualTo("ready");
        assertThat(panel.renderedSessionText()).doesNotContain("openai", "gpt-x", "anthropic", "claude");
        assertThat(descendantText(panel.listElement())).doesNotContain("openai", "gpt-x", "anthropic", "claude");
    }

    @Test
    void emptyRecentSessionsRenderEmptyStateAndNoCards() {
        SessionListPanel panel = new SessionListPanel();

        panel.showRecentSessions(List.of(), null, false);

        assertThat(panel.sessionCards()).isEmpty();
        assertThat(panel.sessionCount()).isZero();
        assertThat(descendantsWithAttribute(panel.listElement(), "data-state", "empty")).hasSize(1);
    }

    @Test
    void cardActivationInvokesHandlerAndSelectionRemainsDeterministic() {
        SessionListPanel panel = new SessionListPanel();
        AtomicReference<String> activated = new AtomicReference<>();
        panel.setSessionActivationHandler(activated::set);
        panel.showRecentSessions(List.of(summary("session-a", "Alpha"), summary("session-b", "Beta")), "session-a", false);

        panel.activateSessionCardForTest("session-a", "click");
        panel.selectSession("session-b");

        assertThat(activated).hasValue("session-a");
        assertThat(panel.sessionCards()).filteredOn(card -> "true".equals(card.getElement().getAttribute("data-session-active")))
                .singleElement()
                .satisfies(card -> assertThat(card.getElement().getAttribute("data-session-id")).isEqualTo("session-b"));
    }

    @Test
    void selectingSessionOutsideBoundedListCreatesMinimalSelectedCard() {
        SessionListPanel panel = new SessionListPanel();
        AtomicReference<String> activated = new AtomicReference<>();
        panel.setSessionActivationHandler(activated::set);
        panel.showRecentSessions(List.of(summary("session-a", "Alpha")), null, false);

        panel.selectSession("session-b");
        panel.activateSessionCardForTest("session-b", "Enter");

        assertThat(panel.recentSessionIds()).containsExactly("session-a", "session-b");
        Div selected = panel.sessionCards().stream()
                .filter(card -> "session-b".equals(card.getElement().getAttribute("data-session-id")))
                .findFirst()
                .orElseThrow();
        assertThat(selected.getElement().getAttribute("data-session-active")).isEqualTo("true");
        assertThat(selected.getElement().getAttribute("role")).isEqualTo("button");
        assertThat(selected.getElement().getAttribute("tabindex")).isEqualTo("0");
        assertThat(activated).hasValue("session-b");
    }

    @Test
    void selectingExistingSummaryKeepsMetadataAndSpaceActivation() {
        SessionListPanel panel = new SessionListPanel();
        AtomicReference<String> activated = new AtomicReference<>();
        panel.setSessionActivationHandler(activated::set);
        panel.showRecentSessions(List.of(summary("session-a", "Alpha"), summary("session-b", "Beta")), null, false);

        panel.selectSession("session-b");
        panel.activateSessionCardForTest("session-b", " ");

        Div selected = panel.sessionCards().stream()
                .filter(card -> "true".equals(card.getElement().getAttribute("data-session-active")))
                .findFirst()
                .orElseThrow();
        assertThat(selected.getElement().getAttribute("data-session-id")).isEqualTo("session-b");
        assertThat(selected.getElement().getAttribute("role")).isEqualTo("button");
        assertThat(selected.getElement().getAttribute("tabindex")).isEqualTo("0");
        assertThat(fieldText(selected, "session-title")).isEqualTo("Beta");
        assertThat(fieldText(selected, "session-preview")).isEqualTo("Preview for Beta");
        assertThat(activated).hasValue("session-b");
    }

    private static SessionSummaryDto summary(String sessionId, String title) {
        return new SessionSummaryDto(
                sessionId,
                title,
                "ready",
                "Preview for " + title,
                Instant.parse("2026-06-23T05:00:00Z"),
                Instant.parse("2026-06-22T05:00:00Z"),
                null,
                "RUNNING",
                Map.of());
    }

    private static String fieldText(Div card, String field) {
        return descendantsWithAttribute(card, "data-field", field).stream()
                .findFirst()
                .map(component -> component.getElement().getTextRecursively())
                .orElseThrow();
    }

    private static String descendantText(Component root) {
        StringBuilder text = new StringBuilder(root.getElement().getTextRecursively());
        root.getChildren().forEach(child -> text.append('\n').append(descendantText(child)));
        return text.toString();
    }

    private static List<Component> descendantsWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = new ArrayList<>();
        root.getChildren().forEach(child -> {
            if (value.equals(child.getElement().getAttribute(attribute))) {
                matches.add(child);
            }
            matches.addAll(descendantsWithAttribute(child, attribute, value));
        });
        return matches;
    }
}
