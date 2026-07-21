package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** Center workbench column that keeps chat messages and runtime events in one narrative. */
public class ChatEventStreamPanel extends Div {

    private final Div feed = new Div();
    private final Div composer = new Div();
    private final Span composerRunStatus = new Span();
    private final TextArea input = new TextArea();
    private final Button send = new Button();
    private final Button composerCancel = new Button();
    private final List<String> messages = new ArrayList<>();
    private final List<Component> eventComponents = new ArrayList<>();
    private final Map<String, LiveAssistantBubble> liveAssistantBubbles = new HashMap<>();
    private Div activeAssistantLine;
    private Consumer<String> submitHandler;
    private Runnable cancelHandler;
    private String streamMode = "polling-fallback";

    public ChatEventStreamPanel() {
        addClassName("pi-console-chat");
        addClassName("pi-chat-home-card");
        getElement().setAttribute("data-column", "chat-event-stream");
        getElement().setAttribute("data-primary", "chat-first");
        feed.addClassName("pi-console-event-feed");
        feed.getElement().setAttribute("data-role", "event-feed");
        composer.addClassName("pi-console-composer");
        composer.getElement().setAttribute("data-role", "chat-composer");
        composer.getElement().setAttribute("data-action-scope", "primary-composer");
        composerRunStatus.setText(t("chat.noActiveRun"));
        composerRunStatus.setVisible(false);
        input.setLabel(t("chat.label.message"));
        input.setPlaceholder(t("chat.placeholder"));
        send.setText(t("chat.button.send"));
        composerCancel.setText(t("chat.button.cancelRun"));
        composerRunStatus.getElement().setAttribute("data-role", "composer-run-status");
        input.setMinRows(2);
        input.setMaxRows(6);
        input.setWidthFull();
        input.getElement().setAttribute("data-role", "chat-input");
        send.getElement().setAttribute("data-action", "send-chat");
        send.addClickListener(event -> submitCurrentInput());
        composerCancel.getElement().setAttribute("data-action", "cancel-run");
        composerCancel.getElement().setAttribute("data-action-compatibility", "cancel-run-primary");
        composerCancel.getElement().setAttribute("data-action-scope", "primary-composer");
        composerCancel.addClickListener(event -> {
            if (cancelHandler != null) {
                cancelHandler.run();
            }
        });
        composerCancel.setVisible(false);
        composer.add(composerRunStatus, input, send, composerCancel);
        add(feed, composer);
        showEmptyState();
    }

    public void appendUserMessage(String text) {
        activeAssistantLine = null;
        liveAssistantBubbles.clear();
        append("user", requireText(text, "text"));
    }

    public void appendUserMessage(String text, String sessionId, String runId) {
        activeAssistantLine = null;
        liveAssistantBubbles.clear();
        appendUserTranscriptMessage(requireText(text, "text"), sessionId, runId);
    }

    public void appendEvent(RunEventRenderer.RenderedEvent event) {
        append(event.category(), event.text(), event.component());
    }

    public void appendSecondaryEvent(RunEventRenderer.RenderedEvent event) {
        if (event == null || event.component() == null) {
            return;
        }
        if (messages.isEmpty() && eventComponents.isEmpty()) {
            feed.removeAll();
        }
        eventComponents.add(event.component());
        feed.add(event.component());
    }

    public String inputPlaceholder() {
        return t("chat.placeholder");
    }

    public int messageCount() {
        return messages.size();
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }

    public void replaceTranscriptForProof(List<ConversationMessageDto> transcriptMessages) {
        replaceTranscript(transcriptMessages);
    }

    public void setStreamMode(String streamMode) {
        this.streamMode = normalizeStreamMode(streamMode);
    }

    public void replaceTranscript(List<ConversationMessageDto> transcriptMessages) {
        feed.removeAll();
        messages.clear();
        eventComponents.clear();
        activeAssistantLine = null;
        liveAssistantBubbles.clear();
        if (transcriptMessages == null || transcriptMessages.isEmpty()) {
            showEmptyState();
            return;
        }
        for (ConversationMessageDto message : transcriptMessages) {
            if (message == null || message.text() == null || message.text().isBlank()) {
                continue;
            }
            appendTranscriptMessage(message);
        }
        if (messages.isEmpty()) {
            showEmptyState();
        }
    }

    public int componentCount() {
        return eventComponents.size();
    }

    public void setSubmitHandler(Consumer<String> submitHandler) {
        this.submitHandler = submitHandler;
    }

    public void setCancelHandler(Runnable cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    public void showComposerRunStatus(String status, boolean cancellable) {
        composerRunStatus.setText(requireText(status, "status"));
        composerRunStatus.setVisible(cancellable);
        composerCancel.setVisible(cancellable);
        composerCancel.getElement().setAttribute("data-prominent", Boolean.toString(cancellable));
    }

    public void showComposerCancelling() {
        showComposerRunStatus(t("console.run.cancelling"), true);
    }

    public String composerStatusText() {
        return composerRunStatus.getText();
    }

    public boolean composerCancelVisible() {
        return composerCancel.isVisible();
    }

    public void beginAssistantMessage(String sessionId, String runId, String stepId) {
        String key = aggregationKey(sessionId, runId, stepId);
        liveAssistantBubbles.computeIfAbsent(key, ignored -> createLiveAssistantBubble(sessionId, runId, stepId, key));
    }

    public void appendAssistantDelta(String sessionId, String runId, String stepId, String delta) {
        String cleanDelta = requireNonBlank(delta, "delta");
        String key = aggregationKey(sessionId, runId, stepId);
        LiveAssistantBubble bubble = liveAssistantBubbles.computeIfAbsent(
                key, ignored -> createLiveAssistantBubble(sessionId, runId, stepId, key));
        if (bubble.terminal()) {
            return;
        }
        bubble.append(cleanDelta);
        bubble.line().removeAll();
        Span text = new Span(bubble.text());
        text.getElement().setAttribute("data-assistant-text", "true");
        bubble.line().add(text);
        setLiveBubbleStatus(bubble.line(), ConversationMessageStatus.PENDING, "streaming");
        if (bubble.messageIndex() == null) {
            bubble.messageIndex(messages.size());
            messages.add(bubble.text());
        } else {
            messages.set(bubble.messageIndex(), bubble.text());
        }
    }

    public void markAssistantTerminal(
            String sessionId,
            String runId,
            String stepId,
            ConversationMessageStatus status,
            String safeSummary) {
        ConversationMessageStatus terminalStatus = status == null ? ConversationMessageStatus.COMPLETED : status;
        String key = aggregationKey(sessionId, runId, stepId);
        LiveAssistantBubble bubble = liveAssistantBubbles.computeIfAbsent(
                key, ignored -> createLiveAssistantBubble(sessionId, runId, stepId, key));
        bubble.terminal(true);
        setLiveBubbleStatus(bubble.line(), terminalStatus, terminalStatus.wireValue());
        if (ConversationMessageStatus.COMPLETED.equals(terminalStatus)) {
            if (bubble.text().isBlank()) {
                bubble.line().removeAll();
                bubble.line().setText("");
            }
            return;
        }
        renderTerminalStatus(bubble, terminalStatus, safeSummary);
    }

    public void showErrorBubble(String sessionId, String runId, String stepId, String safeSummary) {
        markAssistantTerminal(sessionId, runId, stepId, ConversationMessageStatus.FAILED, safeSummary);
    }

    public void markLocalFallbackMode(String label) {
        String text = label == null || label.isBlank() ? t("chat.localFallback.label") : label.trim();
        Div target = activeAssistantLine;
        if (target == null && !liveAssistantBubbles.isEmpty()) {
            target = liveAssistantBubbles.values().stream()
                    .reduce((first, second) -> second)
                    .map(LiveAssistantBubble::line)
                    .orElse(null);
        }
        if (target == null) {
            return;
        }
        target.getElement().setAttribute("data-fallback-mode", "local");
        if (target.getChildren().noneMatch(child -> "fallback-label".equals(child.getElement().getAttribute("data-role")))) {
            Span fallback = new Span(text);
            fallback.getElement().setAttribute("data-role", "fallback-label");
            fallback.getElement().setAttribute("data-fallback-mode", "local");
            target.add(fallback);
        }
    }

    public int inputMinRows() {
        return input.getMinRows();
    }

    public int inputMaxRows() {
        return input.getMaxRows();
    }

    private void submitCurrentInput() {
        String value = input.getValue();
        if (value == null || value.isBlank()) {
            return;
        }
        if (submitHandler != null) {
            submitHandler.accept(value);
            input.clear();
        }
    }

    private void showEmptyState() {
        Span empty = new Span(t("console.session.emptyTranscript"));
        empty.getElement().setAttribute("data-state", "empty");
        feed.add(empty);
    }

    private LiveAssistantBubble createLiveAssistantBubble(String sessionId, String runId, String stepId, String key) {
        if (messages.isEmpty() && eventComponents.isEmpty()) {
            feed.removeAll();
        }
        Div line = new Div();
        line.addClassName("pi-transcript-message");
        line.addClassName("pi-transcript-assistant");
        line.getElement().setAttribute("data-message-role", ConversationMessageRole.ASSISTANT.wireValue());
        line.getElement().setAttribute("data-message-kind", "primary-bubble");
        line.getElement().setAttribute("data-bubble-align", "left");
        line.getElement().setAttribute("data-stream-mode", streamMode);
        line.getElement().setAttribute("data-stream-aggregation-key", key);
        setOptionalAttribute(line, "data-session-id", sessionId);
        setOptionalAttribute(line, "data-run-id", runId);
        setOptionalAttribute(line, "data-step-id", normalizedStepId(stepId));
        setLiveBubbleStatus(line, ConversationMessageStatus.PENDING, ConversationMessageStatus.PENDING.wireValue());
        LiveAssistantBubble bubble = new LiveAssistantBubble(line);
        renderLoadingIndicator(bubble);
        feed.add(line);
        return bubble;
    }

    private void renderLoadingIndicator(LiveAssistantBubble bubble) {
        bubble.line().removeAll();
        bubble.line().getElement().setAttribute("data-loading", "true");
        Div indicator = new Div();
        indicator.getElement().setAttribute("data-role", "assistant-loading");
        Span label = statusChip(ConversationMessageStatus.PENDING, t("chat.assistant.pending"));
        label.getElement().setAttribute("data-loading-label", "true");
        indicator.add(label, loadingDot(), loadingDot(), loadingDot());
        bubble.line().add(indicator);
    }

    private Span loadingDot() {
        Span dot = new Span("•");
        dot.getElement().setAttribute("data-role", "assistant-loading-dot");
        return dot;
    }

    private void renderTerminalStatus(LiveAssistantBubble bubble, ConversationMessageStatus status, String safeSummary) {
        String summary = safeSummary == null || safeSummary.isBlank() ? translatedStatus(status.wireValue()) : safeSummary.trim();
        if (ConversationMessageStatus.FAILED.equals(status) && summary.equals(translatedStatus(status.wireValue()))) {
            summary = t("console.stream.failureSummary");
        }
        bubble.line().removeAll();
        if (!bubble.text().isBlank()) {
            Span text = new Span(bubble.text());
            text.getElement().setAttribute("data-assistant-text", "true");
            bubble.line().add(text);
        }
        bubble.line().add(statusChip(status, summary));
    }

    private Span statusChip(ConversationMessageStatus status, String text) {
        Span chip = new Span(text);
        chip.getElement().setAttribute("data-status-chip", status.wireValue());
        return chip;
    }

    private static void setLiveBubbleStatus(Div line, ConversationMessageStatus status, String streamState) {
        line.getElement().setAttribute("data-message-status", status.wireValue());
        line.getElement().setAttribute("data-stream-state", streamState);
        if ("streaming".equals(streamState) || status != ConversationMessageStatus.PENDING) {
            line.getElement().setAttribute("data-loading", "false");
        }
    }

    private void append(String category, String text) {
        append(category, text, null);
    }

    private void append(String category, String text, Component component) {
        if (messages.isEmpty()) {
            feed.removeAll();
        }
        messages.add(text);
        if ("assistant".equals(category) && activeAssistantLine != null) {
            activeAssistantLine.setText(activeAssistantLine.getText() + text);
            return;
        }
        if (component != null) {
            eventComponents.add(component);
            feed.add(component);
            return;
        }
        Div line = new Div(text);
        line.getElement().setAttribute("data-event-category", category);
        if ("assistant".equals(category)) {
            activeAssistantLine = line;
        }
        feed.add(line);
    }

    private void appendTranscriptMessage(ConversationMessageDto message) {
        String text = requireText(message.text(), "message text");
        ConversationMessageRole role = message.role();
        String roleValue = role == null ? "conversation" : role.wireValue();
        ConversationMessageStatus status = message.status();
        String statusValue = status == null ? "completed" : status.wireValue();
        messages.add(text);
        Div line = new Div();
        line.addClassName("pi-transcript-message");
        line.addClassName("pi-transcript-" + roleValue);
        line.getElement().setAttribute("data-message-role", roleValue);
        line.getElement().setAttribute("data-message-status", statusValue);
        line.getElement().setAttribute("data-stream-state", statusValue);
        setOptionalAttribute(line, "data-session-id", message.sessionId());
        setOptionalAttribute(line, "data-run-id", message.runId());
        if (ConversationMessageRole.TOOL.equals(role) || ConversationMessageRole.ERROR.equals(role)) {
            renderTranscriptCard(line, roleValue, text, statusValue);
        } else if (ConversationMessageRole.USER.equals(role)) {
            line.setText(text);
            line.getElement().setAttribute("data-message-kind", "primary-bubble");
            line.getElement().setAttribute("data-bubble-align", "right");
        } else {
            line.setText(text);
            line.getElement().setAttribute("data-message-kind", "primary-bubble");
            line.getElement().setAttribute("data-bubble-align", "left");
            line.getElement().setAttribute("data-stream-mode", streamMode);
        }
        feed.add(line);
    }

    private static String normalizeStreamMode(String streamMode) {
        if (streamMode == null || streamMode.isBlank()) {
            return "polling-fallback";
        }
        return switch (streamMode.trim()) {
            case "push", "sse", "polling-fallback" -> streamMode.trim();
            default -> "polling-fallback";
        };
    }

    private void appendUserTranscriptMessage(String text, String sessionId, String runId) {
        if (messages.isEmpty() && eventComponents.isEmpty()) {
            feed.removeAll();
        }
        messages.add(text);
        Div line = new Div(text);
        line.addClassName("pi-transcript-message");
        line.addClassName("pi-transcript-user");
        line.getElement().setAttribute("data-message-role", ConversationMessageRole.USER.wireValue());
        line.getElement().setAttribute("data-message-status", ConversationMessageStatus.COMPLETED.wireValue());
        line.getElement().setAttribute("data-stream-state", ConversationMessageStatus.COMPLETED.wireValue());
        line.getElement().setAttribute("data-message-kind", "primary-bubble");
        line.getElement().setAttribute("data-bubble-align", "right");
        setOptionalAttribute(line, "data-session-id", sessionId);
        setOptionalAttribute(line, "data-run-id", runId);
        feed.add(line);
    }

    private void renderTranscriptCard(Div card, String roleValue, String text, String statusValue) {
        card.addClassName("pi-transcript-card");
        card.getElement().setAttribute("data-message-kind", "secondary-card");
        card.getElement().setAttribute("data-transcript-card", roleValue);
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);
        Span label = new Span(roleValue);
        label.getElement().setAttribute("data-card-label", roleValue);
        header.add(label);
        if (!"completed".equals(statusValue)) {
            Span status = new Span(translatedStatus(statusValue));
            status.addClassName("pi-transcript-status");
            status.getElement().setAttribute("data-status-chip", statusValue);
            header.add(status);
        }
        Span summary = new Span(text);
        summary.getElement().setAttribute("data-card-summary", roleValue);
        card.add(header, summary);
        eventComponents.add(card);
    }

    private String translatedStatus(String statusValue) {
        return switch (statusValue) {
            case "pending" -> t("console.stream.pending").toLowerCase(Locale.ROOT);
            case "streaming" -> t("console.stream.streaming").toLowerCase(Locale.ROOT);
            case "completed" -> t("console.stream.completed").toLowerCase(Locale.ROOT);
            case "failed" -> t("console.stream.failed").toLowerCase(Locale.ROOT);
            case "cancelled" -> t("console.stream.cancelled").toLowerCase(Locale.ROOT);
            case "partial" -> t("console.stream.partial").toLowerCase(Locale.ROOT);
            default -> statusValue;
        };
    }

    private String t(String key, Object... params) {
        String translated = getTranslation(key, params);
        if (!translated.startsWith("!{") || !translated.endsWith("}!")) {
            return translated;
        }
        try {
            String pattern = ResourceBundle.getBundle("messages").getString(key);
            return params == null || params.length == 0 ? pattern : MessageFormat.format(pattern, params);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    private static void setOptionalAttribute(Component component, String attribute, String value) {
        if (value != null && !value.isBlank()) {
            component.getElement().setAttribute(attribute, value);
        }
    }

    private static String aggregationKey(String sessionId, String runId, String stepId) {
        return requireText(sessionId, "sessionId") + "::" + requireText(runId, "runId");
    }

    private static String normalizedStepId(String stepId) {
        return stepId == null || stepId.isBlank() ? "default" : stepId.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static final class LiveAssistantBubble {

        private final Div line;
        private final StringBuilder text = new StringBuilder();
        private Integer messageIndex;
        private boolean terminal;

        private LiveAssistantBubble(Div line) {
            this.line = line;
        }

        private Div line() {
            return line;
        }

        private void append(String delta) {
            text.append(delta);
        }

        private String text() {
            return text.toString();
        }

        private Integer messageIndex() {
            return messageIndex;
        }

        private void messageIndex(Integer messageIndex) {
            this.messageIndex = messageIndex;
        }

        private boolean terminal() {
            return terminal;
        }

        private void terminal(boolean terminal) {
            this.terminal = terminal;
        }
    }
}
