package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.adapter.web.ui.PiPageSection;
import java.util.Map;

/** Package-local Admin mobile card/detail helpers for governance views. */
final class AdminMobileCardSupport {

    private AdminMobileCardSupport() {
    }

    static Div page(String routeName, Component... children) {
        Div page = new Div(children == null ? new Component[0] : children);
        page.addClassName("pi-admin-page");
        page.getElement().setAttribute("data-admin-page", requireText(routeName, "routeName"));
        return page;
    }

    static PiPageSection statusCard(String section, String title, String status, String count, String message, Component... details) {
        Div summary = new Div(
                labelValue("Status", status),
                labelValue("Count", count),
                labelValue("Message", message),
                statusChip(status));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection card = PiPageSection.card(requireText(section, "section"), new H3(safeText(title)), summary);
        decorateCard(card, section);
        if (details != null && details.length > 0) {
            card.add(details);
        }
        return card;
    }

    static PiPageSection metricCard(String section, String title, String value, String message, Component... details) {
        Div summary = new Div(labelValue("Value", value), labelValue("Message", message));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection card = PiPageSection.card(requireText(section, "section"), new H3(safeText(title)), summary);
        decorateCard(card, section);
        if (details != null && details.length > 0) {
            card.add(details);
        }
        return card;
    }

    static Div labelValue(String label, String value) {
        Span labelSpan = new Span(safeText(label));
        labelSpan.addClassName("pi-admin-field-label");
        Span valueSpan = new Span(AdminMobileRedactor.boundedStringify(value));
        valueSpan.addClassName("pi-admin-field-value");
        Div row = new Div(labelSpan, valueSpan);
        row.addClassName("pi-admin-field");
        row.getElement().setAttribute("data-admin-field", normalize(label));
        return row;
    }

    static Span statusChip(String status) {
        Span chip = new Span(safeText(status));
        chip.addClassName("pi-status-chip");
        chip.getElement().setAttribute("data-status-chip", safeText(status));
        chip.getElement().setAttribute("data-status-severity", severity(status));
        return chip;
    }

    static Div actionRow(Component... actions) {
        Div row = new Div(actions == null ? new Component[0] : actions);
        row.addClassName("pi-admin-action-row");
        row.getElement().setAttribute("data-admin-actions", "true");
        return row;
    }

    static Anchor actionLink(String href, String text) {
        Anchor anchor = new Anchor(requireText(href, "href"), safeText(text));
        anchor.getElement().setAttribute("data-admin-action-link", href);
        return anchor;
    }

    static Details details(String summary, String layer, Component... content) {
        Div body = new Div(content == null ? new Component[0] : content);
        body.addClassName("pi-detail-block");
        body.getElement().setAttribute("data-detail-layer", layerName(layer));
        Details details = new Details(safeText(summary), body);
        details.setOpened(false);
        details.addClassName("pi-admin-details");
        details.getElement().setAttribute("data-expandable", "true");
        details.getElement().setAttribute("data-admin-details", "true");
        details.getElement().setAttribute("data-detail-layer", layerName(layer));
        return details;
    }

    static Div redactedBlock(Object value) {
        Div block = new Div(new Span(AdminMobileRedactor.boundedStringify(value)));
        block.addClassNames("pi-detail-block", "pi-redacted-json");
        block.getElement().setAttribute("data-detail-layer", "advanced");
        return block;
    }

    static Details metadataDetails(Map<String, String> metadata) {
        Div fields = new Div();
        fields.addClassName("pi-admin-card-grid");
        if (metadata == null || metadata.isEmpty()) {
            fields.add(labelValue("metadata", "none"));
        } else {
            metadata.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> fields.add(labelValue(entry.getKey(), entry.getValue())));
        }
        return details("Metadata", "structured", fields);
    }

    private static void decorateCard(PiPageSection card, String section) {
        card.addClassName("pi-admin-card");
        card.getElement().setAttribute("data-admin-card", "true");
        card.getElement().setAttribute("data-admin-section", requireText(section, "section"));
    }

    private static String severity(String status) {
        String normalized = normalize(status);
        if (normalized.contains("healthy") || normalized.contains("enabled") || normalized.contains("allow") || normalized.contains("up")) {
            return "normal";
        }
        if (normalized.contains("future") || normalized.contains("unconfigured") || normalized.contains("disabled") || normalized.contains("warning")) {
            return "warning";
        }
        return "abnormal";
    }

    private static String layerName(String layer) {
        String normalized = normalize(layer);
        return normalized.isBlank() ? "structured" : normalized;
    }

    private static String normalize(String value) {
        return safeText(value).toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "unknown" : AdminMobileRedactor.redact(value.trim());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
