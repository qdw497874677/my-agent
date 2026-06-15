package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Expandable, redacted governed tool lifecycle card for the integrated Console event stream. */
public class ToolCallCard extends Div {

    private final String summaryText;
    private final String detailsText;

    private ToolCallCard(String summaryText, String detailsText, String status) {
        this.summaryText = summaryText;
        this.detailsText = detailsText;
        addClassName("pi-tool-call-card");
        getElement().setAttribute("data-event-category", "tool");
        getElement().setAttribute("data-tool-status", safe(status));
        getElement().setAttribute("data-expandable", "true");
        add(new Span(summaryText), new Details("Details", new Span(detailsText)));
    }

    public static ToolCallCard from(RunEventDto event) {
        Objects.requireNonNull(event, "event must not be null");
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String toolName = first(payload, "toolName", "tool", "toolId", "descriptorRef");
        String status = first(payload, "status", "decision", "phase");
        String purpose = first(payload, "purpose", "summary", "reason", "policyReason");
        String risk = first(payload, "riskLevel", "risk", "riskLabel");
        String sideEffect = first(payload, "sideEffect", "sideEffectLevel", "sideEffectLabel");
        String progress = first(payload, "progress", "percent", "progressText");
        String result = first(payload, "resultSummary", "outputSummary", "redactedResultSummary");
        String error = first(payload, "errorCategory", "error", "errorSummary");
        String summary = String.join(" | ",
                "Tool " + fallback(toolName, "event"),
                "status=" + fallback(status, event.type()),
                "purpose=" + fallback(purpose, "not specified"),
                "risk=" + fallback(risk, "unknown"),
                "sideEffect=" + fallback(sideEffect, "unknown"),
                "progress=" + fallback(progress, "n/a"),
                "result=" + fallback(result, "pending"),
                "error=" + fallback(error, "none"));
        String details = "sequence=" + event.sequence()
                + " | type=" + safe(event.type())
                + " | payloadSchema=" + safe(event.payloadSchema())
                + " | eventSequence=" + value(payload.get("eventSequence"))
                + " | policyReason=" + fallback(first(payload, "policyReason", "policy", "decisionReason"), "n/a")
                + " | previewId=" + fallback(first(payload, "previewId", "previewRef"), "n/a")
                + " | diagnostics=" + value(payload);
        return new ToolCallCard(summary, details, status);
    }

    public String summaryText() {
        return summaryText;
    }

    public String detailsText() {
        return detailsText;
    }

    private static String first(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value(value);
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> safe(String.valueOf(entry.getKey())) + "=" + value(entry.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(ToolCallCard::value).collect(Collectors.joining(", ", "[", "]"));
        }
        return safe(String.valueOf(value));
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.trim();
        if (looksSensitive(sanitized)) {
            return "[REDACTED]";
        }
        return sanitized;
    }

    private static boolean looksSensitive(String value) {
        String lower = value.toLowerCase();
        return lower.contains("sk-live-")
                || lower.contains("raw-token-value")
                || lower.contains("api_key=")
                || lower.contains("api-key=")
                || lower.contains("password=")
                || lower.contains("secret=")
                || lower.contains("token=");
    }
}
