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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VER-05 proof: a controlled slow stream must make assistant text visible before
 * the terminal completion event arrives. The test intentionally drives the same
 * reducer and ChatEventStreamPanel seams used by Console push/replay handling so
 * it fails if assistant text is buffered and only replayed after completion.
 */
class WebConsoleSlowStreamIncrementalTest {

    @Test
    void assistantTextAppearsBeforeTerminalCompletion() {
        ControlledSlowStream stream = new ControlledSlowStream("session-slow", "run-slow", "step-slow")
                .delta("slow-1", 1, "partial")
                .checkpointBeforeTerminal()
                .delta("slow-2", 2, " text")
                .completed("done-3", 3);
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.begin("session-slow", "run-slow", "step-slow"), panel, renderer);
        SlowStreamObservation beforeTerminal = stream.drainUntilBeforeTerminal(event ->
                ConversationEventReducer.apply(reducer.reduce(event), panel, renderer));

        Component liveAssistant = singlePrimaryAssistantBubble(panel);
        assertThat(beforeTerminal.partial()).isEqualTo("partial");
        assertThat(beforeTerminal.completed()).isFalse();
        assertThat(beforeTerminal.seenTerminal()).isFalse();
        assertThat(liveAssistant.getElement().getTextRecursively()).isEqualTo("partial");
        assertThat(liveAssistant.getElement().getAttribute("data-stream-state")).isEqualTo("streaming");
        assertThat(liveAssistant.getElement().getAttribute("data-message-status")).isEqualTo("pending");
        assertThat(panel.messages()).containsExactly("partial");

        stream.drainRemaining(event -> ConversationEventReducer.apply(reducer.reduce(event), panel, renderer));

        Component completedAssistant = singlePrimaryAssistantBubble(panel);
        assertThat(completedAssistant).isSameAs(liveAssistant);
        assertThat(completedAssistant.getElement().getTextRecursively()).isEqualTo("partial text");
        assertThat(completedAssistant.getElement().getAttribute("data-stream-state")).isEqualTo("completed");
        assertThat(completedAssistant.getElement().getAttribute("data-message-status"))
                .isEqualTo(ConversationMessageStatus.COMPLETED.wireValue());
        assertThat(panel.messages()).containsExactly("partial text");
    }

    @Test
    void beforeTerminalProofFailsIfOnlyCompletionIsDelivered() {
        ControlledSlowStream completionOnly = new ControlledSlowStream("session-slow", "run-complete-only", "step-slow")
                .checkpointBeforeTerminal()
                .completed("done-1", 1);
        ConversationEventReducer reducer = new ConversationEventReducer();
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer();

        ConversationEventReducer.apply(reducer.begin("session-slow", "run-complete-only", "step-slow"), panel, renderer);
        SlowStreamObservation beforeTerminal = completionOnly.drainUntilBeforeTerminal(event ->
                ConversationEventReducer.apply(reducer.reduce(event), panel, renderer));

        Component assistant = singlePrimaryAssistantBubble(panel);
        assertThat(beforeTerminal.partial()).isBlank();
        assertThat(beforeTerminal.seenTerminal()).isFalse();
        assertThat(assistant.getElement().getAttribute("data-stream-state")).isEqualTo("pending");
        assertThat(panel.messages()).isEmpty();
    }

    private static Component singlePrimaryAssistantBubble(ChatEventStreamPanel panel) {
        List<Component> assistants = primaryAssistantBubbles(panel);
        assertThat(assistants).hasSize(1);
        return assistants.getFirst();
    }

    private static List<Component> primaryAssistantBubbles(ChatEventStreamPanel panel) {
        return descendants(panel.getElement())
                .filter(element -> "assistant".equals(element.getAttribute("data-message-role")))
                .filter(element -> "primary-bubble".equals(element.getAttribute("data-message-kind")))
                .flatMap(element -> element.getComponent().stream())
                .toList();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren()
                .flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static RunEventDto event(
            String eventId,
            String sessionId,
            String runId,
            String stepId,
            long sequence,
            String type,
            Map<String, Object> payload) {
        return new RunEventDto(eventId, "tenant-1", "user-1", sessionId, runId, stepId, "workspace-1",
                sequence, Instant.parse("2026-06-01T00:00:00Z"), type, "trace-1", "correlation-1", null,
                "USER", null, "schema", 1, payload);
    }

    @FunctionalInterface
    private interface SlowEventConsumer {
        void accept(RunEventDto event);
    }

    private record SlowStreamObservation(String partial, boolean completed, boolean seenTerminal) {
    }

    private static final class ControlledSlowStream {
        private final String sessionId;
        private final String runId;
        private final String stepId;
        private final List<SlowFrame> frames = new ArrayList<>();
        private int cursor;

        private ControlledSlowStream(String sessionId, String runId, String stepId) {
            this.sessionId = sessionId;
            this.runId = runId;
            this.stepId = stepId;
        }

        private ControlledSlowStream delta(String eventId, long sequence, String partial) {
            frames.add(SlowFrame.delta(event(eventId, sessionId, runId, stepId, sequence, "model.delta", Map.of("text", partial)), partial));
            return this;
        }

        private ControlledSlowStream checkpointBeforeTerminal() {
            frames.add(SlowFrame.beforeTerminalMarker());
            return this;
        }

        private ControlledSlowStream completed(String eventId, long sequence) {
            frames.add(SlowFrame.completed(event(eventId, sessionId, runId, stepId, sequence, "run.completed", Map.of())));
            return this;
        }

        private SlowStreamObservation drainUntilBeforeTerminal(SlowEventConsumer consumer) {
            StringBuilder partial = new StringBuilder();
            boolean completed = false;
            boolean seenTerminal = false;
            while (cursor < frames.size()) {
                SlowFrame frame = frames.get(cursor++);
                if (frame.beforeTerminal()) {
                    return new SlowStreamObservation(partial.toString(), completed, seenTerminal);
                }
                seenTerminal = seenTerminal || frame.terminal();
                completed = completed || frame.completed();
                if (frame.partial() != null) {
                    partial.append(frame.partial());
                }
                consumer.accept(frame.event());
            }
            return new SlowStreamObservation(partial.toString(), completed, seenTerminal);
        }

        private void drainRemaining(SlowEventConsumer consumer) {
            while (cursor < frames.size()) {
                SlowFrame frame = frames.get(cursor++);
                if (!frame.beforeTerminal()) {
                    consumer.accept(frame.event());
                }
            }
        }
    }

    private record SlowFrame(RunEventDto event, String partial, boolean beforeTerminal, boolean terminal, boolean completed) {
        private static SlowFrame delta(RunEventDto event, String partial) {
            return new SlowFrame(event, partial, false, false, false);
        }

        private static SlowFrame beforeTerminalMarker() {
            return new SlowFrame(null, null, true, false, false);
        }

        private static SlowFrame completed(RunEventDto event) {
            return new SlowFrame(event, null, false, true, true);
        }
    }
}
