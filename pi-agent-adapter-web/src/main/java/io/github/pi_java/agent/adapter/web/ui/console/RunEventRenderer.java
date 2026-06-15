package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.client.event.RunEventDto;
import java.util.Map;

/** Renders public RunEvent DTOs into user-visible integrated chat/event stream lines. */
public class RunEventRenderer {

    public RenderedEvent render(RunEventDto event) {
        String type = event.type() == null ? "run.event" : event.type();
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String lower = type.toLowerCase();
        if (lower.contains("model.delta")) {
            return new RenderedEvent("model", value(payload, "text", "delta", "content"), false);
        }
        if (lower.contains("tool.lifecycle")) {
            String tool = value(payload, "toolName", "tool", "toolId");
            String status = value(payload, "status", "decision", "phase");
            return new RenderedEvent("tool", "Tool " + fallback(tool, "event") + ": " + fallback(status, type), terminal(lower, payload));
        }
        if (lower.contains("policy")) {
            return new RenderedEvent("policy", "Policy: " + value(payload, "decision", "reason", "status"), terminal(lower, payload));
        }
        if (lower.contains("completed") || lower.contains("failed") || lower.contains("cancelled")) {
            return new RenderedEvent("terminal", "Run terminal: " + value(payload, "status", "reason", "message"), true);
        }
        if (lower.contains("status")) {
            return new RenderedEvent("status", "Run status: " + value(payload, "status", "state", "message"), terminal(lower, payload));
        }
        return new RenderedEvent("event", type + ": " + payload, terminal(lower, payload));
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

    public record RenderedEvent(String category, String text, boolean terminal) {
    }
}
