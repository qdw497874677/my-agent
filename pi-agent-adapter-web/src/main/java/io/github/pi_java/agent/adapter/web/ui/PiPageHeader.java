package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

/** Shared page header primitive with title, subtitle/status, and primary action slot semantics. */
public class PiPageHeader extends Div {

    private final H1 title = new H1();
    private final Span subtitle = new Span();
    private final Div status = new Div();
    private final Div actions = new Div();

    public PiPageHeader(String titleText) {
        addClassName("pi-page-header");
        title.addClassName("pi-page-header-title");
        title.getElement().setAttribute("data-page-title", "header");
        title.setText(requireText(titleText, "titleText"));
        subtitle.addClassName("pi-page-header-subtitle");
        subtitle.getElement().setAttribute("data-page-subtitle", "header");
        status.addClassName("pi-page-header-status");
        status.getElement().setAttribute("data-page-status", "header");
        actions.addClassName("pi-page-header-actions");
        actions.getElement().setAttribute("data-primary-action", "page-header");
        add(title, subtitle, status, actions);
    }

    public PiPageHeader withSubtitle(String text) {
        subtitle.setText(requireText(text, "subtitle"));
        return this;
    }

    public PiPageHeader withStatus(Component component) {
        status.removeAll();
        if (component != null) {
            status.add(component);
        }
        return this;
    }

    public PiPageHeader withPrimaryAction(Component component) {
        actions.removeAll();
        if (component != null) {
            actions.add(component);
        }
        return this;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
