package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Left workbench column for recent sessions and continue-session selection. */
public class SessionListPanel extends Div {

    private static final String EMPTY = "No recent sessions yet. Start a chat to create one.";

    private final Div list = new Div();
    private final List<String> sessionIds = new ArrayList<>();
    private final List<String> renderedSessionText = new ArrayList<>();
    private final List<Div> sessionCards = new ArrayList<>();
    private final Map<String, SessionMetadata> sessionMetadata = new LinkedHashMap<>();
    private Consumer<String> sessionActivationHandler;
    private String selectedSessionId;

    public SessionListPanel() {
        addClassName("pi-console-sessions");
        getElement().setAttribute("data-column", "sessions");
        list.getElement().setAttribute("data-role", "session-list");
        add(new H2("Sessions"), list);
        renderEmpty();
    }

    public void showSession(String sessionId, String title, Instant updatedAt) {
        String id = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(id)) {
            sessionIds.add(id);
        }
        sessionMetadata.put(id, new SessionMetadata(
                title == null || title.isBlank() ? "Recent session" : title,
                "ready",
                updatedAt == null ? "not yet updated" : updatedAt.toString()));
        renderList();
    }

    public void selectSession(String sessionId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(selectedSessionId)) {
            sessionIds.add(selectedSessionId);
        }
        sessionMetadata.put(selectedSessionId, new SessionMetadata("Selected session", "ready", "not yet updated"));
        renderList();
    }

    public String emptyStateText() {
        return EMPTY;
    }

    public int sessionCount() {
        return sessionIds.size();
    }

    public List<String> recentSessionIds() {
        return List.copyOf(sessionIds);
    }

    public String renderedSessionText() {
        return String.join("\n", renderedSessionText);
    }

    public String selectedSessionId() {
        return selectedSessionId;
    }

    public Div listElement() {
        return list;
    }

    public List<Div> sessionCards() {
        return List.copyOf(sessionCards);
    }

    public void setSessionActivationHandler(Consumer<String> sessionActivationHandler) {
        this.sessionActivationHandler = sessionActivationHandler;
    }

    public void activateSessionCardForTest(String sessionId, String activation) {
        if (activation == null || activation.isEmpty()) {
            throw new IllegalArgumentException("activation must not be empty");
        }
        String key = activation;
        if ("click".equalsIgnoreCase(key) || "Enter".equals(key) || " ".equals(key) || "Space".equals(key)) {
            activateSessionCard(sessionId);
            return;
        }
        throw new IllegalArgumentException("Unsupported activation: " + activation);
    }

    private void renderEmpty() {
        list.removeAll();
        sessionCards.clear();
        Span empty = new Span(EMPTY);
        empty.getElement().setAttribute("data-state", "empty");
        list.add(empty);
    }

    private void renderList() {
        list.removeAll();
        renderedSessionText.clear();
        sessionCards.clear();
        for (String id : sessionIds) {
            SessionMetadata metadata = sessionMetadata.getOrDefault(
                    id, new SessionMetadata("Recent session", "ready", "not yet updated"));
            String text = (Objects.equals(id, selectedSessionId) ? "▶ " : "")
                    + id
                    + " · "
                    + metadata.title()
                    + " · "
                    + metadata.status()
                    + " · "
                    + metadata.updatedAt();
            renderedSessionText.add(text);
            Div card = new Div(
                    field("session-title", metadata.title()),
                    field("session-status", metadata.status()),
                    field("session-updated-at", metadata.updatedAt()));
            card.addClassName("pi-session-card");
            card.getElement().setAttribute("data-role", "session-card");
            card.getElement().setAttribute("data-session-id", id);
            card.getElement().setAttribute("data-session-active", Boolean.toString(Objects.equals(id, selectedSessionId)));
            card.getElement().setAttribute("role", "button");
            card.getElement().setAttribute("tabindex", "0");
            card.addClickListener(event -> activateSessionCard(id));
            card.getElement()
                    .addEventListener("keydown", event -> activateSessionCard(id))
                    .setFilter("event.key === 'Enter' || event.key === ' ' || event.code === 'Space'");
            sessionCards.add(card);
            list.add(card);
        }
    }

    private void activateSessionCard(String sessionId) {
        String id = requireText(sessionId, "sessionId");
        if (sessionActivationHandler != null) {
            sessionActivationHandler.accept(id);
            return;
        }
        selectSession(id);
    }

    private static Span field(String name, String value) {
        Span span = new Span(value == null || value.isBlank() ? fallbackFor(name) : value);
        span.getElement().setAttribute("data-field", name);
        return span;
    }

    private static String fallbackFor(String name) {
        return switch (name) {
            case "session-title" -> "Recent session";
            case "session-updated-at" -> "not yet updated";
            default -> "ready";
        };
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private record SessionMetadata(String title, String status, String updatedAt) {
    }
}
