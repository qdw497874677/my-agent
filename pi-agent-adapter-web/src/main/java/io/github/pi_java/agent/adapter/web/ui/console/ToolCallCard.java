package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.util.Map;
import java.util.Objects;

/** Expandable, redacted governed tool lifecycle card for the integrated Console event stream. */
public class ToolCallCard extends Div {

    private final String summaryText;
    private final String detailsText;

    private ToolCallCard(String summaryText, String structuredText, String detailsText, String status, String toolName, String source, String policy) {
        this.summaryText = summaryText;
        this.detailsText = detailsText;
        addClassNames("pi-tool-call-card", "pi-card");
        getElement().setAttribute("data-event-category", "tool");
        getElement().setAttribute("data-tool-status", safe(status));
        getElement().setAttribute("data-tool-name", safe(toolName));
        getElement().setAttribute("data-tool-source", safe(source));
        getElement().setAttribute("data-policy-state", safe(policy));
        getElement().setAttribute("data-expandable", "true");
        add(summary(summaryText), details("Input / output summary", "structured", structuredText),
                details("Advanced redacted detail", "advanced", detailsText));
    }

    public static ToolCallCard from(RunEventDto event) {
        Objects.requireNonNull(event, "event must not be null");
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String toolName = first(payload, "toolName", "tool", "toolId", "descriptorRef");
        String source = first(payload, "source", "provider", "registrySource", "toolSource");
        String status = first(payload, "status", "decision", "phase");
        String policy = first(payload, "policyDecision", "policyState", "policyReason", "decision");
        String approval = first(payload, "approvalState", "approvalStatus", "requiresApproval", "previewId");
        String duration = first(payload, "durationMs", "durationMillis", "elapsedMs", "duration");
        String purpose = first(payload, "purpose", "summary", "reason", "resultSummary", "outputSummary", "redactedResultSummary");
        String risk = first(payload, "riskLevel", "risk", "riskLabel");
        String sideEffect = first(payload, "sideEffect", "sideEffectLevel", "sideEffectLabel");
        String progress = first(payload, "progress", "percent", "progressText");
        String result = first(payload, "resultSummary", "outputSummary", "redactedResultSummary");
        String error = first(payload, "errorCategory", "error", "errorSummary");
        String summary = String.join(" | ",
                "Tool: " + fallback(toolName, "event"),
                "Source: " + fallback(source, "runtime"),
                "Status: " + fallback(status, event.type()),
                "Policy: " + fallback(policy, "unknown"),
                "Approval: " + fallback(approval, "unknown"),
                "Duration: " + fallback(formatDuration(duration), "n/a"),
                "Error: " + fallback(error, "none"),
                "Summary: " + fallback(purpose, "not specified"),
                "Risk: " + fallback(risk, "unknown"),
                "Side effect: " + fallback(sideEffect, "unknown"),
                "Progress: " + fallback(progress, "n/a"),
                "Result: " + fallback(result, "pending"));
        String structured = String.join(" | ",
                "inputSummary=" + fallback(first(payload, "inputSummary"), "n/a"),
                "argumentSummary=" + fallback(first(payload, "argumentSummary"), "n/a"),
                "outputSummary=" + fallback(first(payload, "outputSummary"), "n/a"),
                "resultSummary=" + fallback(first(payload, "resultSummary", "redactedResultSummary"), "n/a"),
                "preview=" + fallback(value(payload.get("preview")), "n/a"),
                "diagnostics=" + fallback(value(payload.get("diagnostics")), "n/a"));
        String details = "sequence=" + event.sequence()
                + " | type=" + safe(event.type())
                + " | payloadSchema=" + safe(event.payloadSchema())
                + " | eventSequence=" + value(payload.get("eventSequence"))
                + " | policyReason=" + fallback(first(payload, "policyReason", "policy", "decisionReason"), "n/a")
                + " | previewId=" + fallback(first(payload, "previewId", "previewRef"), "n/a")
                + " | diagnostics=" + value(payload);
        return new ToolCallCard(summary, structured, details, status, toolName, fallback(source, "runtime"), policy);
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
        return RuntimeDetailRedactor.stringify(value);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return RuntimeDetailRedactor.redact(value.trim());
    }

    private Div summary(String text) {
        Div container = new Div();
        container.addClassName("pi-tool-call-card__summary");
        container.add(new Span(text));
        return container;
    }

    private Details details(String summary, String layer, String text) {
        Details details = new Details(summary, new Span(text));
        details.addClassName("pi-detail");
        details.getElement().setAttribute("data-detail-layer", layer);
        return details;
    }

    private static String formatDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return "";
        }
        String trimmed = duration.trim();
        return trimmed.matches("\\d+") ? trimmed + "ms" : trimmed;
    }
}
