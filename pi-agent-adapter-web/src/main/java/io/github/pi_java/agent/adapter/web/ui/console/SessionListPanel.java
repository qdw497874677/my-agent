package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Left workbench column for recent sessions and continue-session selection. */
public class SessionListPanel extends Div {

    private static final String EMPTY = "No recent sessions yet. Start a chat to create one.";

    private final Div list = new Div();
    private final List<String> sessionIds = new ArrayList<>();
    private final List<String> renderedSessionText = new ArrayList<>();
    private String selectedSessionId;

    public SessionListPanel() {
        addClassName("pi-console-sessions");
        getElement().setAttribute("data-column", "sessions");
        add(new H2("Sessions"), list);
        renderEmpty();
    }

    public void showSession(String sessionId, String title, Instant updatedAt) {
        String id = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(id)) {
            sessionIds.add(id);
        }
        renderList(title == null || title.isBlank() ? "Recent session" : title, updatedAt);
    }

    public void selectSession(String sessionId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(selectedSessionId)) {
            sessionIds.add(selectedSessionId);
        }
        renderList("Selected session", null);
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

    private void renderEmpty() {
        list.removeAll();
        Span empty = new Span(EMPTY);
        empty.getElement().setAttribute("data-state", "empty");
        list.add(empty);
    }

    private void renderList(String fallbackTitle, Instant updatedAt) {
        list.removeAll();
        renderedSessionText.clear();
        for (String id : sessionIds) {
            String text = (Objects.equals(id, selectedSessionId) ? "▶ " : "")
                    + id
                    + " · "
                    + fallbackTitle
                    + (updatedAt == null ? "" : " · " + updatedAt);
            renderedSessionText.add(text);
            Div button = new Div(text);
            button.getElement().setAttribute("data-session-id", id);
            button.getElement().setAttribute("role", "button");
            list.add(button);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
