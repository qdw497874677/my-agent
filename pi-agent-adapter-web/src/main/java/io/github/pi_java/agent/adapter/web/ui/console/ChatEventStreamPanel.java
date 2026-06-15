package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.List;

/** Center workbench column that keeps chat messages and runtime events in one narrative. */
public class ChatEventStreamPanel extends Div {

    private static final String PLACEHOLDER = "Type a message for the selected Agent…";

    private final Div stream = new Div();
    private final TextArea input = new TextArea("Message");
    private final Button send = new Button("Send");
    private final List<String> messages = new ArrayList<>();
    private final List<Component> eventComponents = new ArrayList<>();

    public ChatEventStreamPanel() {
        addClassName("pi-console-chat");
        getElement().setAttribute("data-column", "chat-event-stream");
        getElement().setAttribute("data-primary", "chat-first");
        input.setPlaceholder(PLACEHOLDER);
        input.getElement().setAttribute("data-role", "chat-input");
        send.getElement().setAttribute("data-action", "send-chat");
        add(new H2("Chat"), stream, input, send);
        showEmptyState();
    }

    public void appendUserMessage(String text) {
        append("user", requireText(text, "text"));
    }

    public void appendEvent(RunEventRenderer.RenderedEvent event) {
        append(event.category(), event.text(), event.component());
    }

    public String inputPlaceholder() {
        return PLACEHOLDER;
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

    private void showEmptyState() {
        Span empty = new Span("Start with a message or continue a session from the left.");
        empty.getElement().setAttribute("data-state", "empty");
        stream.add(empty);
    }

    private void append(String category, String text) {
        append(category, text, null);
    }

    private void append(String category, String text, Component component) {
        if (messages.isEmpty()) {
            stream.removeAll();
        }
        messages.add(text);
        if (component != null) {
            eventComponents.add(component);
            stream.add(component);
            return;
        }
        Div line = new Div(text);
        line.getElement().setAttribute("data-event-category", category);
        stream.add(line);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
