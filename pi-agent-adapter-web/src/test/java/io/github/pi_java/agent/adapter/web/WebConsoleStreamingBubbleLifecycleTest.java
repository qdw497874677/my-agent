package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConversationEventReducer;
import io.github.pi_java.agent.adapter.web.ui.console.ConversationEventReducer.Operation;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.event.RunEventDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleStreamingBubbleLifecycleTest {

    @Test
    void beginAssistantMessageCreatesOnePendingPrimaryAssistantBubbleWithStableSelectors() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        Component assistant = assistants.getFirst();
        assertThat(assistant.getElement().getAttribute("data-message-role")).isEqualTo("assistant");
        assertThat(assistant.getElement().getAttribute("data-message-kind")).isEqualTo("primary-bubble");
        assertThat(assistant.getElement().getAttribute("data-session-id")).isEqualTo("session-1");
        assertThat(assistant.getElement().getAttribute("data-run-id")).isEqualTo("run-1");
        assertThat(assistant.getElement().getAttribute("data-step-id")).isEqualTo("step-1");
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("pending");
        assertThat(assistant.getElement().getAttribute("data-message-status")).isEqualTo("pending");
        assertThat(assistant.getElement().getAttribute("data-loading")).isEqualTo("true");
        assertThat(assistant.getElement().getAttribute("data-stream-aggregation-key")).isEqualTo("session-1::run-1");
        assertThat(descendantsWithAttribute(assistant, "data-role").stream()
                .filter(component -> "assistant-loading".equals(component.getElement().getAttribute("data-role"))))
                .hasSize(1);
        assertThat(descendantsWithAttribute(assistant, "data-role").stream()
                .filter(component -> "assistant-loading-dot".equals(component.getElement().getAttribute("data-role"))))
                .hasSize(3);
    }

    @Test
    void placeholderAndProviderEventsForSameRunCoalesceEvenWhenStepIdsDiffer() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "placeholder-step");
        panel.appendAssistantDelta("session-1", "run-1", "provider-step", "Alpha");
        panel.appendAssistantDelta("session-1", "run-1", "provider-step", " Beta");
        panel.markAssistantTerminal("session-1", "run-1", "provider-step", ConversationMessageStatus.COMPLETED, null);

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-aggregation-key")).isEqualTo("session-1::run-1");
        assertThat(assistants.getFirst().getElement().getAttribute("data-step-id")).isEqualTo("placeholder-step");
        assertThat(assistants.getFirst().getElement().getTextRecursively()).isEqualTo("Alpha Beta");
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("completed");
    }

    @Test
    void assistantDeltasAppendToSameBubbleWithoutIncreasingPrimaryAssistantCount() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");
        panel.appendAssistantDelta("session-1", "run-1", "step-1", "Hello");
        panel.appendAssistantDelta("session-1", "run-1", "step-1", " world");

        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getTextRecursively()).isEqualTo("Hello world");
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("streaming");
        assertThat(assistants.getFirst().getElement().getAttribute("data-loading")).isEqualTo("false");
        assertThat(descendantsWithAttribute(assistants.getFirst(), "data-role").stream()
                .noneMatch(component -> "assistant-loading".equals(component.getElement().getAttribute("data-role"))))
                .isTrue();
        assertThat(panel.messages()).containsExactly("Hello world");
    }

    @Test
    void terminalStatesMutateExistingAssistantBubbleWithoutBlankMessages() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        panel.beginAssistantMessage("session-1", "run-1", "step-1");
        panel.markAssistantTerminal("session-1", "run-1", "step-1", ConversationMessageStatus.COMPLETED, null);

        assertThat(primaryAssistantBubbles(panel)).hasSize(1);
        Component completed = primaryAssistantBubbles(panel).getFirst();
        assertThat(completed.getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(completed.getElement().getAttribute("data-message-status")).isEqualTo("completed");
        assertThat(panel.messages()).isEmpty();

        ChatEventStreamPanel failedPanel = new ChatEventStreamPanel();
        failedPanel.beginAssistantMessage("session-1", "run-failed", "step-1");
        failedPanel.appendAssistantDelta("session-1", "run-failed", "step-1", "Partial text");
        failedPanel.markAssistantTerminal("session-1", "run-failed", "step-1", ConversationMessageStatus.FAILED, "Provider unavailable");
        Component failed = primaryAssistantBubbles(failedPanel).getFirst();
        assertThat(failed.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(failed.getElement().getTextRecursively()).contains("Partial text", "Provider unavailable");
        assertThat(primaryAssistantBubbles(failedPanel)).hasSize(1);

        ChatEventStreamPanel cancelledPanel = new ChatEventStreamPanel();
        cancelledPanel.beginAssistantMessage("session-1", "run-cancelled", "step-1");
        cancelledPanel.markAssistantTerminal("session-1", "run-cancelled", "step-1", ConversationMessageStatus.CANCELLED, null);
        assertThat(primaryAssistantBubbles(cancelledPanel).getFirst().getElement().getAttribute("data-stream-state"))
                .isEqualTo("cancelled");

        ChatEventStreamPanel partialPanel = new ChatEventStreamPanel();
        partialPanel.beginAssistantMessage("session-1", "run-partial", "step-1");
        partialPanel.markAssistantTerminal("session-1", "run-partial", "step-1", ConversationMessageStatus.PARTIAL, null);
        assertThat(primaryAssistantBubbles(partialPanel).getFirst().getElement().getAttribute("data-stream-state"))
                .isEqualTo("partial");
    }

    @Test
    void reducerIgnoresDuplicateEventIdOrAlreadyRenderedRunSequence() {
        ConversationEventReducer reducer = new ConversationEventReducer();

        Operation first = reducer.reduce(event("event-1", 10, "model.delta", Map.of("text", "A")));
        Operation duplicateId = reducer.reduce(event("event-1", 11, "model.delta", Map.of("text", "B")));
        Operation duplicateSequence = reducer.reduce(event("event-2", 10, "model.delta", Map.of("text", "C")));

        assertThat(first.kind()).isEqualTo(Operation.Kind.APPEND_ASSISTANT_DELTA);
        assertThat(first.delta()).isEqualTo("A");
        assertThat(duplicateId.kind()).isEqualTo(Operation.Kind.IGNORE);
        assertThat(duplicateSequence.kind()).isEqualTo(Operation.Kind.IGNORE);
    }

    @Test
    void reducerAppendsNonEmptyModelDeltaPayloadFieldsAndIgnoresBlankFinishChunks() {
        ConversationEventReducer reducer = new ConversationEventReducer();

        assertThat(reducer.reduce(event("event-1", 1, "model.delta", Map.of("text", "A"))).delta()).isEqualTo("A");
        assertThat(reducer.reduce(event("event-2", 2, "model.delta", Map.of("textDelta", "B"))).delta()).isEqualTo("B");
        assertThat(reducer.reduce(event("event-3", 3, "model.delta", Map.of("delta", "C"))).delta()).isEqualTo("C");
        assertThat(reducer.reduce(event("event-4", 4, "model.delta", Map.of("content", "D"))).delta()).isEqualTo("D");
        assertThat(reducer.reduce(event("event-5", 5, "model.delta", Map.of("finishReason", "stop"))).kind())
                .isEqualTo(Operation.Kind.IGNORE);
    }

    @Test
    void reducerRoutesToolApprovalPolicyAndRuntimeEventsAsSecondaryOperations() {
        ConversationEventReducer reducer = new ConversationEventReducer();

        assertThat(reducer.reduce(event("tool-1", 1, "tool.lifecycle", Map.of("status", "started"))).kind())
                .isEqualTo(Operation.Kind.SECONDARY_EVENT);
        assertThat(reducer.reduce(event("approval-1", 2, "approval_required", Map.of("status", "APPROVAL_REQUIRED"))).kind())
                .isEqualTo(Operation.Kind.SECONDARY_EVENT);
        assertThat(reducer.reduce(event("policy-1", 3, "policy.decision", Map.of("decision", "allow"))).kind())
                .isEqualTo(Operation.Kind.SECONDARY_EVENT);
        assertThat(reducer.reduce(event("runtime-1", 4, "runtime.status", Map.of("status", "running"))).kind())
                .isEqualTo(Operation.Kind.SECONDARY_EVENT);
    }

    @Test
    void reducerProducesTerminalOperationsAndIgnoresLaterDeltasForTerminalKey() {
        ConversationEventReducer reducer = new ConversationEventReducer();

        Operation completed = reducer.reduce(event("done-1", 1, "run.completed", Map.of()));
        assertThat(completed.kind()).isEqualTo(Operation.Kind.MARK_TERMINAL);
        assertThat(completed.status()).isEqualTo(ConversationMessageStatus.COMPLETED);

        Operation failed = reducer.reduce(event("failed-1", 1, "run.failed", Map.of("message", "Provider unavailable"), "run-failed"));
        assertThat(failed.kind()).isEqualTo(Operation.Kind.MARK_TERMINAL);
        assertThat(failed.status()).isEqualTo(ConversationMessageStatus.FAILED);
        assertThat(failed.safeSummary()).isEqualTo("Provider unavailable");
        assertThat(reducer.reduce(event("late-1", 2, "model.delta", Map.of("text", "late"), "run-failed")).kind())
                .isEqualTo(Operation.Kind.IGNORE);

        Operation cancelled = reducer.reduce(event("cancel-1", 1, "run.cancelled", Map.of("reason", "user cancelled"), "run-cancelled"));
        assertThat(cancelled.status()).isEqualTo(ConversationMessageStatus.CANCELLED);
    }

    @Test
    void reducerOperationsApplyToPanelAsOneAssistantBubbleWithSecondaryCards() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        apply(reducer.reduce(event("delta-1", 1, "model.delta", Map.of("text", "A"))), panel, renderer);
        apply(reducer.reduce(event("delta-1", 2, "model.delta", Map.of("text", "A"))), panel, renderer);
        apply(reducer.reduce(event("tool-1", 3, "tool.lifecycle", Map.of("status", "started", "toolName", "lookup"))), panel, renderer);
        apply(reducer.reduce(event("delta-2", 4, "model.delta", Map.of("text", "B"))), panel, renderer);
        apply(reducer.reduce(event("done-1", 5, "run.completed", Map.of())), panel, renderer);

        assertThat(primaryAssistantBubbles(panel)).hasSize(1);
        assertThat(primaryAssistantBubbles(panel).getFirst().getElement().getTextRecursively()).isEqualTo("AB");
        assertThat(primaryAssistantBubbles(panel).getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(panel.messages()).containsExactly("AB");
        assertThat(panel.componentCount()).isEqualTo(1);
    }

    @Test
    void failedTerminalRendersSafeSummaryWithoutRawPayloadOrSecrets() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        apply(reducer.reduce(event("delta-1", 1, "model.delta", Map.of("text", "partial"), "run-failed")), panel, renderer);
        apply(reducer.reduce(event("failed-1", 2, "run.failed",
                Map.of("message", "Provider unavailable", "apiKey", "sk-secret", "metadata", Map.of("token", "raw-token")),
                "run-failed")), panel, renderer);

        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("failed");
        assertThat(assistant.getElement().getTextRecursively()).contains("partial", "Provider unavailable");
        assertThat(assistant.getElement().getTextRecursively())
                .doesNotContain("apiKey", "sk-secret", "metadata", "raw-token", "{", "}");
    }

    @Test
    void cancelledTerminalPreservesPartialTextAndIgnoresLaterDelta() {
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        apply(reducer.reduce(event("delta-1", 1, "model.delta", Map.of("text", "partial"), "run-cancelled")), panel, renderer);
        apply(reducer.reduce(event("cancel-1", 2, "run.cancelled", Map.of("reason", "user cancelled"), "run-cancelled")), panel, renderer);
        apply(reducer.reduce(event("delta-2", 3, "model.delta", Map.of("text", " late"), "run-cancelled")), panel, renderer);

        Component assistant = primaryAssistantBubbles(panel).getFirst();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("cancelled");
        assertThat(assistant.getElement().getTextRecursively()).contains("partial", "user cancelled");
        assertThat(assistant.getElement().getTextRecursively()).doesNotContain("late");
        assertThat(panel.messages()).containsExactly("partial");
    }

    private static List<Component> primaryAssistantBubbles(ChatEventStreamPanel panel) {
        return descendantsWithAttribute(panel, "data-message-role").stream()
                .filter(component -> "assistant".equals(component.getElement().getAttribute("data-message-role")))
                .filter(component -> "primary-bubble".equals(component.getElement().getAttribute("data-message-kind")))
                .toList();
    }

    private static List<Component> descendantsWithAttribute(Component root, String attribute) {
        return descendants(root.getElement())
                .filter(element -> element.hasAttribute(attribute))
                .flatMap(element -> element.getComponent().stream())
                .toList();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static void apply(Operation operation, ChatEventStreamPanel panel, RunEventRenderer renderer) {
        ConversationEventReducer.apply(operation, panel, renderer);
    }

    private static RunEventDto event(String eventId, long sequence, String type, Map<String, Object> payload) {
        return event(eventId, sequence, type, payload, "run-1");
    }

    private static RunEventDto event(String eventId, long sequence, String type, Map<String, Object> payload, String runId) {
        return new RunEventDto(eventId, "tenant-1", "user-1", "session-1", runId, "step-1", "workspace-1",
                sequence, Instant.parse("2026-06-01T00:00:00Z"), type, "trace-1", "correlation-1", null,
                "USER", null, "schema", 1, payload);
    }
}
