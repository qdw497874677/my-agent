package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical transcript reduction logic for the Phase 16 conversation read
 * model (decisions D-05, D-06, D-07, D-08, D-13, D-16).
 *
 * <p>Folds a session's run inputs and persisted run events into stable typed
 * {@link ConversationMessageDto} entries. The assembler is a pure function of
 * its inputs: it performs no I/O and depends only on App, Domain, Client, and
 * JDK types. Output is restricted to Plan 01 typed client DTOs; it never
 * produces raw {@code Map<String,Object>} history entries.
 *
 * <p>Reduction rules:
 * <ul>
 *   <li>A run's input map (key {@code text}, falling back to {@code prompt})
 *       becomes one {@link ConversationMessageRole#USER} message marked
 *       {@link ConversationMessageStatus#COMPLETED}, ordered first within its
 *       run (D-10).</li>
 *   <li>{@link RunEventType#MODEL_DELTA} events accumulate a single
 *       {@link ConversationMessageRole#ASSISTANT} message preserving the
 *       combined text and the first/last delta sequence range.</li>
 *   <li>{@link RunEventType#RUN_COMPLETED}, {@link RunEventType#RUN_FAILED},
 *       and {@link RunEventType#RUN_CANCELLED} events are state transitions,
 *       not blank assistant messages: they set the assistant status to
 *       completed/failed/cancelled respectively when accumulated assistant
 *       text exists. A failure with no assistant text produces an
 *       {@link ConversationMessageRole#ERROR} message carrying a safe summary
 *       (D-07).</li>
 *   <li>{@link RunEventPayload.ToolLifecyclePayload} events become
 *       {@link ConversationMessageRole#TOOL} messages with redacted summary
 *       metadata; raw executor/class/secret/token fields are stripped (D-06,
 *       existing redaction discipline).</li>
 * </ul>
 *
 * <p>Within a run, event-derived messages are ordered by event sequence; across
 * runs, messages are ordered by run creation timestamp (D-09, D-16).
 */
public final class ConversationTranscriptAssembler {

    private static final String NO_STEP = "step-none";

    /**
     * Assemble typed transcript messages for a session.
     *
     * @param sessionId the owning session identifier.
     * @param runs      the session's runs (carrying input and creation time).
     * @param events    the full set of run events to fold (may span multiple runs).
     * @return typed transcript messages in conversation order, never {@code null}.
     */
    public List<ConversationMessageDto> assemble(String sessionId,
                                                 List<ConversationRunView> runs,
                                                 List<RunEvent> events) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        List<ConversationRunView> sortedRuns = new ArrayList<>(runs == null ? List.of() : runs);
        sortedRuns.sort(Comparator
                .comparing(ConversationRunView::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ConversationRunView::runId));
        Map<String, List<RunEvent>> eventsByRun = groupByRun(events == null ? List.of() : events);

        List<ConversationMessageDto> messages = new ArrayList<>();
        for (ConversationRunView run : sortedRuns) {
            messages.addAll(buildMessagesForRun(sessionId, run, eventsByRun.getOrDefault(run.runId(), List.of())));
        }
        return messages;
    }

    private List<ConversationMessageDto> buildMessagesForRun(String sessionId,
                                                             ConversationRunView run,
                                                             List<RunEvent> events) {
        List<RunEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparingLong(RunEvent::sequence));

        List<ConversationMessageDto> messages = new ArrayList<>();

        String userInput = extractInputText(run.input());
        if (userInput != null && !userInput.isBlank()) {
            messages.add(new ConversationMessageDto(
                    messageId(sessionId, run.runId(), "user"),
                    sessionId, run.runId(), null,
                    ConversationMessageRole.USER, userInput, ConversationMessageStatus.COMPLETED,
                    run.createdAt(), run.createdAt(),
                    null, null, Map.of(), true, false));
        }

        StringBuilder assistantText = new StringBuilder();
        Long assistantFirst = null;
        Long assistantLast = null;
        Instant assistantCreatedAt = null;
        Instant assistantUpdatedAt = null;
        String assistantStepId = null;
        ConversationMessageStatus assistantStatus = null;
        boolean assistantVisible = true;
        boolean assistantRedacted = false;

        List<EventMessage> eventDerived = new ArrayList<>();

        for (RunEvent event : sorted) {
            RunEventPayload payload = event.payload();
            if (payload instanceof RunEventPayload.ModelDeltaPayload delta) {
                if (delta.textDelta() != null && !delta.textDelta().isBlank()) {
                    if (assistantFirst == null) {
                        assistantFirst = event.sequence();
                        assistantCreatedAt = event.timestamp();
                        assistantStepId = stepIdOrNone(event);
                        assistantVisible = isVisible(event);
                    }
                    assistantText.append(delta.textDelta());
                    assistantLast = event.sequence();
                    assistantUpdatedAt = event.timestamp();
                    if (assistantStatus == null) {
                        assistantStatus = ConversationMessageStatus.PENDING;
                    }
                    assistantRedacted = assistantRedacted || event.redaction().redacted();
                }
            } else if (payload instanceof RunEventPayload.ToolLifecyclePayload tool) {
                eventDerived.add(new EventMessage(event.sequence(), toolMessage(sessionId, run.runId(), event, tool)));
            } else if (payload instanceof RunEventPayload.RunLifecyclePayload) {
                ConversationMessageStatus terminal = terminalStatus(event.type());
                if (terminal != null) {
                    if (assistantFirst != null) {
                        assistantStatus = terminal;
                    } else if (terminal == ConversationMessageStatus.FAILED) {
                        eventDerived.add(new EventMessage(event.sequence(),
                                errorMessage(sessionId, run.runId(), event, (RunEventPayload.RunLifecyclePayload) payload)));
                    }
                }
            }
        }

        if (assistantFirst != null && assistantText.length() > 0) {
            ConversationMessageDto assistant = new ConversationMessageDto(
                    messageId(sessionId, run.runId(), "assistant"),
                    sessionId, run.runId(), assistantStepId,
                    ConversationMessageRole.ASSISTANT, assistantText.toString(),
                    assistantStatus != null ? assistantStatus : ConversationMessageStatus.COMPLETED,
                    assistantCreatedAt, assistantUpdatedAt,
                    assistantFirst, assistantLast, Map.of(), assistantVisible, assistantRedacted);
            eventDerived.add(new EventMessage(assistantFirst, assistant));
        }

        eventDerived.sort(Comparator.comparingLong(EventMessage::sortSequence));
        for (EventMessage derived : eventDerived) {
            messages.add(derived.message());
        }
        return messages;
    }

    private ConversationMessageDto toolMessage(String sessionId, String runId, RunEvent event,
                                               RunEventPayload.ToolLifecyclePayload tool) {
        Map<String, Object> metadata = safeToolMetadata(tool);
        String executionStatus = tool.executionStatus().map(Enum::name).orElse("UNKNOWN");
        String text = tool.errorCategory().isPresent()
                ? "Tool " + tool.toolId() + " failed"
                : "Tool " + tool.toolId() + " (" + executionStatus + ")";
        ConversationMessageStatus status = tool.errorCategory().isPresent()
                ? ConversationMessageStatus.FAILED
                : ConversationMessageStatus.COMPLETED;
        return new ConversationMessageDto(
                messageId(sessionId, runId, "tool:" + event.sequence()),
                sessionId, runId, stepIdOrNone(event),
                ConversationMessageRole.TOOL, text, status,
                event.timestamp(), event.timestamp(),
                event.sequence(), event.sequence(), metadata, isVisible(event), true);
    }

    private ConversationMessageDto errorMessage(String sessionId, String runId, RunEvent event,
                                                RunEventPayload.RunLifecyclePayload lifecycle) {
        String summary = lifecycle.failureSummary() != null ? lifecycle.failureSummary().message() : "Run failed";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("safeSummary", summary);
        return new ConversationMessageDto(
                messageId(sessionId, runId, "error:" + event.sequence()),
                sessionId, runId, stepIdOrNone(event),
                ConversationMessageRole.ERROR, summary, ConversationMessageStatus.FAILED,
                event.timestamp(), event.timestamp(),
                event.sequence(), event.sequence(), redactMap(metadata), isVisible(event), true);
    }

    private Map<String, Object> safeToolMetadata(RunEventPayload.ToolLifecyclePayload tool) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("toolId", tool.toolId());
        safe.put("toolCallId", tool.toolCallId());
        tool.executionStatus().map(Enum::name).ifPresent(s -> safe.put("executionStatus", s));
        tool.policyDecision().map(Enum::name).ifPresent(d -> safe.put("policyDecision", d));
        tool.errorCategory().ifPresent(c -> safe.put("errorCategory", c));
        safe.put("inputSummary", redactMap(tool.redactedInputSummary()));
        safe.put("outputSummary", redactMap(tool.redactedOutputSummary()));
        return redactMap(safe);
    }

    private static ConversationMessageStatus terminalStatus(RunEventType type) {
        return switch (type) {
            case RUN_COMPLETED -> ConversationMessageStatus.COMPLETED;
            case RUN_FAILED -> ConversationMessageStatus.FAILED;
            case RUN_CANCELLED -> ConversationMessageStatus.CANCELLED;
            default -> null;
        };
    }

    private static Map<String, List<RunEvent>> groupByRun(List<RunEvent> events) {
        Map<String, List<RunEvent>> byRun = new LinkedHashMap<>();
        for (RunEvent event : events) {
            byRun.computeIfAbsent(event.runId().value(), ignored -> new ArrayList<>()).add(event);
        }
        return byRun;
    }

    private static String extractInputText(Map<String, Object> input) {
        Object value = input.get("text");
        if (value == null) {
            value = input.get("prompt");
        }
        return value == null ? null : value.toString();
    }

    private static boolean isVisible(RunEvent event) {
        return event.visibility() == EventVisibility.USER;
    }

    private static String stepIdOrNone(RunEvent event) {
        String value = event.stepId().value();
        return (value == null || value.isBlank() || NO_STEP.equals(value)) ? null : value;
    }

    private static String messageId(String sessionId, String runId, String suffix) {
        return sessionId + ":" + runId + ":" + suffix;
    }

    private static Map<String, Object> redactMap(Map<String, Object> values) {
        Map<String, Object> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (!isSensitiveKey(key)) {
                safe.put(key, safeValue(value));
            }
        });
        return Map.copyOf(safe);
    }

    private static Object safeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) -> {
                String key = String.valueOf(nestedKey);
                if (!isSensitiveKey(key)) {
                    nested.put(key, safeValue(nestedValue));
                }
            });
            return Map.copyOf(nested);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.contains("executor")
                || normalized.contains("class");
    }

    private record EventMessage(long sortSequence, ConversationMessageDto message) {
    }
}
