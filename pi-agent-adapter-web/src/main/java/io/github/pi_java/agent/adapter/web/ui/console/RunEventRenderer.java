package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.Component;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.util.Map;
import java.util.Set;

/** Renders public RunEvent DTOs into user-visible integrated chat/event stream lines. */
public class RunEventRenderer {

    private final ConsoleHttpClient httpClient;

    public RunEventRenderer() {
        this(new ConsoleHttpClient());
    }

    public RunEventRenderer(ConsoleHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public RenderedEvent render(RunEventDto event) {
        String type = event.type() == null ? "run.event" : event.type();
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String lower = type.toLowerCase();
        if (lower.contains("model.delta")) {
            return new RenderedEvent("model", value(payload, "text", "delta", "content"), false);
        }
        if (lower.contains("approval_required") || "APPROVAL_REQUIRED".equalsIgnoreCase(value(payload, "status"))) {
            ApprovalCard card = ApprovalCard.from(toApprovalSummary(event, payload), httpClient);
            return new RenderedEvent("approval", card.summaryText(), false, card);
        }
        if (lower.contains("tool.lifecycle") || "tool.lifecycle".equalsIgnoreCase(event.payloadSchema())) {
            ToolCallCard card = ToolCallCard.from(event);
            return new RenderedEvent("tool", card.summaryText(), terminal(lower, payload), card);
        }
        if (lower.contains("policy")) {
            return new RenderedEvent("policy", "Policy: " + value(payload, "decision", "reason", "status"), terminal(lower, payload), null);
        }
        if (lower.contains("completed") || lower.contains("failed") || lower.contains("cancelled")) {
            return new RenderedEvent("terminal", "Run terminal: " + value(payload, "status", "reason", "message"), true, null);
        }
        if (lower.contains("status")) {
            return new RenderedEvent("status", "Run status: " + value(payload, "status", "state", "message"), terminal(lower, payload), null);
        }
        return new RenderedEvent("event", type + ": " + payload, terminal(lower, payload), null);
    }

    private static String value(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean terminal(String type, Map<String, Object> payload) {
        Object status = payload.get("status");
        String normalized = status == null ? type : String.valueOf(status).toLowerCase();
        return normalized.contains("completed")
                || normalized.contains("failed")
                || normalized.contains("cancelled")
                || normalized.contains("timed_out");
    }

    @SuppressWarnings("unchecked")
    private static ApprovalSummaryDto toApprovalSummary(RunEventDto event, Map<String, Object> payload) {
        Map<String, Object> preview = map(payload.get("preview"));
        Map<String, Object> arguments = map(payload.get("argumentSummary"));
        String approvalId = fallback(value(payload, "approvalId", "previewId"), value(preview, "previewId", "id"));
        if (approvalId.isBlank()) {
            approvalId = fallback(value(payload, "toolCallId"), event.eventId());
        }
        String toolCallId = value(payload, "toolCallId", "tool_call_id");
        String toolId = value(payload, "toolId", "tool", "descriptorRef");
        String toolName = value(payload, "toolName", "tool", "descriptorRef");
        return new ApprovalSummaryDto(
                event.sessionId(),
                event.runId(),
                approvalId,
                toolCallId,
                toolId,
                toolName,
                value(payload, "policyReason", "reason", "decisionReason"),
                value(payload, "riskLabel", "riskLevel", "risk"),
                value(payload, "sideEffectLabel", "sideEffect", "sideEffectLevel"),
                preview,
                arguments,
                fallback(value(payload, "expectedConsequence"), "Approve resumes the gated tool path; reject records a same-run policy outcome."),
                true,
                Set.of("USER", "ADMIN"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    public record RenderedEvent(String category, String text, boolean terminal, Component component) {
        public RenderedEvent(String category, String text, boolean terminal) {
            this(category, text, terminal, null);
        }
    }
}
