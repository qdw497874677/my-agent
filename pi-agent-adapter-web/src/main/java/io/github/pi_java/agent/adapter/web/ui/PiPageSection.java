package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/** Shared card/detail surface primitive for incremental Console/Admin mobile migrations. */
public class PiPageSection extends Div {

    public enum Variant {
        CARD("pi-card"),
        DETAIL("pi-detail");

        private final String cssClass;

        Variant(String cssClass) {
            this.cssClass = cssClass;
        }
    }

    public PiPageSection(String sectionName, Variant variant, Component... children) {
        addClassName(variant == null ? Variant.CARD.cssClass : variant.cssClass);
        getElement().setAttribute("data-section", requireText(sectionName, "sectionName"));
        if (children != null) {
            add(children);
        }
    }

    public static PiPageSection card(String sectionName, Component... children) {
        return new PiPageSection(sectionName, Variant.CARD, children);
    }

    public static PiPageSection detail(String sectionName, Component... children) {
        return new PiPageSection(sectionName, Variant.DETAIL, children);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
