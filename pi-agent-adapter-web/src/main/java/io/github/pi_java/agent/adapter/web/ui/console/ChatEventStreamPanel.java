package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.List;
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
    private Consumer<String> submitHandler;
    private Runnable cancelHandler;

    public ChatEventStreamPanel() {
        addClassName("pi-console-chat");
        getElement().setAttribute("data-column", "chat-event-stream");
        getElement().setAttribute("data-primary", "chat-first");
        feed.addClassName("pi-console-event-feed");
        feed.getElement().setAttribute("data-role", "event-feed");
        composer.addClassName("pi-console-composer");
        composer.getElement().setAttribute("data-role", "chat-composer");
        composerRunStatus.setText(getTranslation("chat.noActiveRun"));
        input.setLabel(getTranslation("chat.label.message"));
        input.setPlaceholder(getTranslation("chat.placeholder"));
        send.setText(getTranslation("chat.button.send"));
        composerCancel.setText(getTranslation("chat.button.cancelRun"));
        composerRunStatus.getElement().setAttribute("data-role", "composer-run-status");
        input.setMinRows(2);
        input.setMaxRows(6);
        input.getElement().setAttribute("data-role", "chat-input");
        send.getElement().setAttribute("data-action", "send-chat");
        send.addClickListener(event -> submitCurrentInput());
        composerCancel.getElement().setAttribute("data-action", "cancel-run-primary");
        composerCancel.addClickListener(event -> {
            if (cancelHandler != null) {
                cancelHandler.run();
            }
        });
        composerCancel.setVisible(false);
        composer.add(composerRunStatus, input, send, composerCancel);
        add(new H2(getTranslation("chat.title")), feed, composer);
        showEmptyState();
    }

    public void appendUserMessage(String text) {
        append("user", requireText(text, "text"));
    }

    public void appendEvent(RunEventRenderer.RenderedEvent event) {
        append(event.category(), event.text(), event.component());
    }

    public String inputPlaceholder() {
        return getTranslation("chat.placeholder");
    }

    public int messageCount() {
        return messages.size();
    }

    public List<String> messages() {
        return List.copyOf(messages);
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
        composerCancel.setVisible(cancellable);
        composerCancel.getElement().setAttribute("data-prominent", Boolean.toString(cancellable));
    }

    public void showComposerCancelling() {
        showComposerRunStatus(getTranslation("console.run.cancelling"), true);
    }

    public String composerStatusText() {
        return composerRunStatus.getText();
    }

    public boolean composerCancelVisible() {
        return composerCancel.isVisible();
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
        Span empty = new Span(getTranslation("chat.empty"));
        empty.getElement().setAttribute("data-state", "empty");
        feed.add(empty);
    }

    private void append(String category, String text) {
        append(category, text, null);
    }

    private void append(String category, String text, Component component) {
        if (messages.isEmpty()) {
            feed.removeAll();
        }
        messages.add(text);
        if (component != null) {
            eventComponents.add(component);
            feed.add(component);
            return;
        }
        Div line = new Div(text);
        line.getElement().setAttribute("data-event-category", category);
        feed.add(line);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
