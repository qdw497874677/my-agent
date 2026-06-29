package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Left workbench column for recent sessions and continue-session selection. */
public class SessionListPanel extends Div {

    private final Div list = new Div();
    private final List<String> sessionIds = new ArrayList<>();
    private final List<String> renderedSessionText = new ArrayList<>();
    private final List<Div> sessionCards = new ArrayList<>();
    private final Map<String, SessionMetadata> sessionMetadata = new LinkedHashMap<>();
    private Consumer<String> sessionActivationHandler;
    private String selectedSessionId;
    private boolean hasMoreRecentSessions;

    public SessionListPanel() {
        addClassName("pi-console-sessions");
        getElement().setAttribute("data-column", "sessions");
        list.getElement().setAttribute("data-role", "session-list");
        add(new H2(getTranslation("sessions.title")), list);
        renderEmpty();
    }

    public void showSession(String sessionId, String title, Instant updatedAt) {
        showSession(sessionId, title, "ready", updatedAt);
    }

    public void showSession(String sessionId, String title, String status, Instant updatedAt) {
        String id = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(id)) {
            sessionIds.add(id);
        }
        SessionMetadata existing = sessionMetadata.get(id);
        sessionMetadata.put(id, new SessionMetadata(
                title == null || title.isBlank() ? existing == null ? getTranslation("sessions.defaultTitle") : existing.title() : title,
                existing == null ? getTranslation("sessions.defaultPreview") : existing.preview(),
                status == null || status.isBlank() ? existing == null ? getTranslation("sessions.defaultStatus") : existing.status() : status,
                updatedAt == null ? existing == null ? getTranslation("sessions.defaultUpdated") : existing.updatedAt() : updatedAt.toString()));
        renderList();
    }

    public void showRecentSessionsForProof(List<SessionSummaryDto> summaries) {
        showRecentSessions(summaries, selectedSessionId, false);
    }

    public void showRecentSessions(List<SessionSummaryDto> summaries, String selectedSessionId, boolean hasMore) {
        sessionIds.clear();
        sessionMetadata.clear();
        this.hasMoreRecentSessions = hasMore;
        this.selectedSessionId = selectedSessionId == null || selectedSessionId.isBlank() ? null : selectedSessionId.trim();
        for (SessionSummaryDto summary : summaries == null ? List.<SessionSummaryDto>of() : summaries) {
            if (summary == null || summary.sessionId() == null || summary.sessionId().isBlank()) {
                continue;
            }
            String id = summary.sessionId().trim();
            sessionIds.add(id);
            sessionMetadata.put(id, fromSummary(summary));
        }
        if (this.selectedSessionId != null && !sessionIds.contains(this.selectedSessionId)) {
            sessionIds.add(this.selectedSessionId);
            sessionMetadata.putIfAbsent(this.selectedSessionId, selectedFallbackMetadata());
        }
        if (sessionIds.isEmpty()) {
            renderEmpty();
        } else {
            renderList();
        }
    }

    public void selectSession(String sessionId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        if (!sessionIds.contains(selectedSessionId)) {
            sessionIds.add(selectedSessionId);
        }
        sessionMetadata.putIfAbsent(selectedSessionId, selectedFallbackMetadata());
        renderList();
    }

    public String emptyStateText() {
        return getTranslation("sessions.empty");
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
        Span empty = new Span(getTranslation("sessions.empty"));
        empty.getElement().setAttribute("data-state", "empty");
        list.add(empty);
    }

    private void renderList() {
        list.removeAll();
        renderedSessionText.clear();
        sessionCards.clear();
        for (String id : sessionIds) {
            SessionMetadata metadata = sessionMetadata.getOrDefault(
                    id,
                    new SessionMetadata(
                            getTranslation("sessions.defaultTitle"),
                            getTranslation("sessions.defaultPreview"),
                            getTranslation("sessions.defaultStatus"),
                            getTranslation("sessions.defaultUpdated")));
            String text = (Objects.equals(id, selectedSessionId) ? "▶ " : "")
                    + id
                    + " · "
                    + metadata.title()
                    + " · "
                    + metadata.preview()
                    + " · "
                    + metadata.status()
                    + " · "
                    + metadata.updatedAt();
            renderedSessionText.add(text);
            Div card = new Div(
                    field("session-title", metadata.title()),
                    field("session-preview", metadata.preview()),
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
        if (hasMoreRecentSessions) {
            Span more = new Span(getTranslation("sessions.more"));
            more.getElement().setAttribute("data-role", "session-more");
            list.add(more);
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

    private Span field(String name, String value) {
        Span span = new Span(value == null || value.isBlank() ? fallbackFor(name) : value);
        span.getElement().setAttribute("data-field", name);
        return span;
    }

    private String fallbackFor(String name) {
        return switch (name) {
            case "session-title" -> getTranslation("sessions.defaultTitle");
            case "session-updated-at" -> getTranslation("sessions.defaultUpdated");
            case "session-preview" -> getTranslation("sessions.defaultPreview");
            default -> getTranslation("sessions.defaultStatus");
        };
    }

    private SessionMetadata fromSummary(SessionSummaryDto summary) {
        String status = summary.activeRunStatus() == null || summary.activeRunStatus().isBlank()
                ? summary.status()
                : summary.activeRunStatus();
        return new SessionMetadata(
                summary.title(),
                summary.lastMessagePreview(),
                status,
                summary.lastActivityAt() == null ? getTranslation("sessions.defaultUpdated") : summary.lastActivityAt().toString());
    }

    private SessionMetadata selectedFallbackMetadata() {
        return new SessionMetadata(
                getTranslation("sessions.selected"),
                getTranslation("sessions.defaultPreview"),
                getTranslation("sessions.defaultStatus"),
                getTranslation("sessions.defaultUpdated"));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private record SessionMetadata(String title, String preview, String status, String updatedAt) {
    }
}
