package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.util.Map;
import java.util.Objects;

/** Compact timeline-style runtime card rendered inside the existing Console event feed. */
public class RuntimeEventCard extends Div {

    private final String summaryText;
    private final String detailsText;
    private final String advancedDetailText;

    private RuntimeEventCard(RunEventDto event, String category, String status, String summary) {
        Objects.requireNonNull(event, "event must not be null");
        String eventType = fallback(event.type(), "run.event");
        this.summaryText = String.join(" | ",
                "status=" + fallback(status, "unknown"),
                "type=" + eventType,
                "timestamp=" + event.timestamp(),
                "summary=" + RuntimeDetailRedactor.shorten(fallback(summary, eventType), 180));
        this.detailsText = buildDetails(event, summary);
        this.advancedDetailText = RuntimeDetailRedactor.boundedStringify(event.payload());

        addClassName("pi-runtime-event-card");
        getElement().setAttribute("data-event-category", fallback(category, "event"));
        getElement().setAttribute("data-event-type", eventType);
        getElement().setAttribute("data-event-status", fallback(status, "unknown"));
        getElement().setAttribute("data-expandable", "true");
        getElement().setAttribute("data-layered-detail", "true");
        add(new Span(summaryText),
                new Details("Details", new Span(detailsText)),
                new Details("Advanced redacted detail", new Span(advancedDetailText)));
    }

    public static RuntimeEventCard from(RunEventDto event, String category, String status, String summary) {
        return new RuntimeEventCard(event, category, status, summary);
    }

    public String summaryText() {
        return summaryText;
    }

    public String detailsText() {
        return detailsText;
    }

    public String advancedDetailText() {
        return advancedDetailText;
    }

    private static String buildDetails(RunEventDto event, String summary) {
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        return "sequence=" + event.sequence()
                + " | session=" + RuntimeDetailRedactor.stringify(event.sessionId())
                + " | run=" + RuntimeDetailRedactor.stringify(event.runId())
                + " | type=" + RuntimeDetailRedactor.stringify(event.type())
                + " | summary=" + RuntimeDetailRedactor.shorten(summary, 220)
                + " | payload=" + RuntimeDetailRedactor.boundedStringify(payload);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : RuntimeDetailRedactor.redact(value.trim());
    }
}
