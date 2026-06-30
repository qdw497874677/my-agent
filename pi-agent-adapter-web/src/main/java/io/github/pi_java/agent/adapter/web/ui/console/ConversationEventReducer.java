package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.event.RunEventDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministically reduces public run events into chat-panel operations without
 * depending on Vaadin component state.
 */
public class ConversationEventReducer {

    private final Map<String, RunState> runStates = new HashMap<>();
    private final Set<String> terminalKeys = new HashSet<>();

    public Operation reduce(RunEventDto event) {
        if (event == null) {
            return Operation.ignore();
        }
        String key = aggregationKey(event.sessionId(), event.runId(), event.stepId());
        RunState state = runStates.computeIfAbsent(runStateKey(event), ignored -> new RunState());
        if (isDuplicate(event, state)) {
            return Operation.ignore();
        }
        remember(event, state);
        if (terminalKeys.contains(key)) {
            return Operation.ignore();
        }
        String type = normalize(event.type());
        if (type.contains("model.delta")) {
            String delta = payloadValue(event.payload(), "text", "textDelta", "delta", "content");
            return delta.isBlank() ? Operation.ignore() : Operation.appendAssistantDelta(event, key, delta);
        }
        if (type.contains("run.completed")) {
            terminalKeys.add(key);
            return Operation.markTerminal(event, key, ConversationMessageStatus.COMPLETED, null);
        }
        if (type.contains("run.failed") || type.contains("failed") || type.contains("timed_out")) {
            terminalKeys.add(key);
            return Operation.markTerminal(event, key, ConversationMessageStatus.FAILED,
                    safeSummary(event.payload(), "message", "reason", "status", "error"));
        }
        if (type.contains("run.cancelled") || type.contains("cancelled")) {
            terminalKeys.add(key);
            return Operation.markTerminal(event, key, ConversationMessageStatus.CANCELLED,
                    safeSummary(event.payload(), "reason", "message", "status"));
        }
        if (isSecondaryEvent(type, event)) {
            return Operation.secondaryEvent(event, key);
        }
        return Operation.secondaryEvent(event, key);
    }

    public Operation begin(String sessionId, String runId, String stepId) {
        return Operation.beginAssistant(sessionId, runId, stepId, aggregationKey(sessionId, runId, stepId));
    }

    public static void apply(Operation operation, ChatEventStreamPanel panel, RunEventRenderer runEventRenderer) {
        if (operation == null || panel == null) {
            return;
        }
        switch (operation.kind()) {
            case BEGIN_ASSISTANT -> panel.beginAssistantMessage(operation.sessionId(), operation.runId(), operation.stepId());
            case APPEND_ASSISTANT_DELTA -> panel.appendAssistantDelta(
                    operation.sessionId(), operation.runId(), operation.stepId(), operation.delta());
            case MARK_TERMINAL -> panel.markAssistantTerminal(
                    operation.sessionId(), operation.runId(), operation.stepId(), operation.status(), operation.safeSummary());
            case SECONDARY_EVENT -> {
                if (runEventRenderer != null && operation.event() != null) {
                    RunEventRenderer.RenderedEvent rendered = runEventRenderer.render(operation.event());
                    if (rendered.component() != null) {
                        panel.appendSecondaryEvent(rendered);
                    } else if (!rendered.text().isBlank()) {
                        panel.appendEvent(rendered);
                    }
                }
            }
            case IGNORE -> {
                // Intentionally no-op for replay duplicates, blank finish chunks, and post-terminal deltas.
            }
        }
    }

    private static boolean isDuplicate(RunEventDto event, RunState state) {
        String eventId = event.eventId();
        if (eventId != null && !eventId.isBlank() && state.renderedEventIds.contains(eventId)) {
            return true;
        }
        return event.sequence() <= state.highestRenderedSequence;
    }

    private static void remember(RunEventDto event, RunState state) {
        String eventId = event.eventId();
        if (eventId != null && !eventId.isBlank()) {
            state.renderedEventIds.add(eventId);
        }
        state.highestRenderedSequence = Math.max(state.highestRenderedSequence, event.sequence());
    }

    private static boolean isSecondaryEvent(String type, RunEventDto event) {
        String payloadSchema = normalize(event.payloadSchema());
        String status = normalize(payloadValue(event.payload(), "status"));
        return type.contains("tool")
                || payloadSchema.contains("tool.lifecycle")
                || type.contains("approval")
                || status.contains("approval_required")
                || type.contains("policy")
                || type.contains("runtime")
                || type.contains("status");
    }

    private static String safeSummary(Map<String, Object> payload, String... keys) {
        String value = payloadValue(payload, keys);
        return value.isBlank() ? null : value;
    }

    private static String payloadValue(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text) {
                return text;
            }
            if (value instanceof Number || value instanceof Boolean) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private static String aggregationKey(String sessionId, String runId, String stepId) {
        return normalizeKeyPart(sessionId) + "::" + normalizeKeyPart(runId) + "::" + normalizeStepId(stepId);
    }

    private static String runStateKey(RunEventDto event) {
        return normalizeKeyPart(event.sessionId()) + "::" + normalizeKeyPart(event.runId());
    }

    private static String normalizeKeyPart(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String normalizeStepId(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class RunState {
        private final Set<String> renderedEventIds = new HashSet<>();
        private long highestRenderedSequence = Long.MIN_VALUE;
    }

    public record Operation(
            Kind kind,
            String sessionId,
            String runId,
            String stepId,
            String aggregationKey,
            String delta,
            ConversationMessageStatus status,
            String safeSummary,
            RunEventDto event) {

        public enum Kind {
            BEGIN_ASSISTANT,
            APPEND_ASSISTANT_DELTA,
            MARK_TERMINAL,
            SECONDARY_EVENT,
            IGNORE
        }

        public static Operation ignore() {
            return new Operation(Kind.IGNORE, null, null, null, null, null, null, null, null);
        }

        public static Operation appendAssistantDelta(RunEventDto event, String key, String delta) {
            return new Operation(Kind.APPEND_ASSISTANT_DELTA, event.sessionId(), event.runId(), event.stepId(), key,
                    delta, null, null, event);
        }

        public static Operation beginAssistant(String sessionId, String runId, String stepId, String key) {
            return new Operation(Kind.BEGIN_ASSISTANT, sessionId, runId, stepId, key,
                    null, null, null, null);
        }

        public static Operation markTerminal(
                RunEventDto event,
                String key,
                ConversationMessageStatus status,
                String safeSummary) {
            return new Operation(Kind.MARK_TERMINAL, event.sessionId(), event.runId(), event.stepId(), key,
                    null, status, safeSummary, event);
        }

        public static Operation secondaryEvent(RunEventDto event, String key) {
            return new Operation(Kind.SECONDARY_EVENT, event.sessionId(), event.runId(), event.stepId(), key,
                    null, null, null, event);
        }
    }
}
