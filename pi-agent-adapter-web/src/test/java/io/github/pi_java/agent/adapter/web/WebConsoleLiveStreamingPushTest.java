package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.adapter.web.ui.PiWebAppShell;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleLiveRunEventSubscriber;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class WebConsoleLiveStreamingPushTest {

    @AfterEach
    void clearCurrentUi() {
        UI.setCurrent(null);
    }

    @Test
    void appShellEnablesVaadinPushForProductStreamingPath() {
        assertThat(PiWebAppShell.class.getAnnotation(Push.class)).isNotNull();
    }

    @Test
    void liveSubscriberDispatchesFanoutEventsThroughUiAccessAndClosesOnDetach() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        RecordingUi ui = installUi();
        Component owner = new com.vaadin.flow.component.html.Div();
        ui.add(owner);
        List<RunEventDto> delivered = new ArrayList<>();

        ConsoleLiveRunEventSubscriber subscriber = new ConsoleLiveRunEventSubscriber(fanout);
        subscriber.subscribe(owner, "run-1", delivered::add);

        assertThat(fanout.subscriberCount("run-1")).isEqualTo(1);
        fanout.publish(domainEvent("event-1", "session-1", "run-1", 1, "model.delta", Map.of("text", "A")));

        assertThat(ui.accessCalls()).isEqualTo(1);
        assertThat(delivered).extracting(RunEventDto::eventId).containsExactly("event-1");

        ui.remove(owner);

        assertThat(fanout.subscriberCount("run-1")).isZero();
        fanout.publish(domainEvent("event-2", "session-1", "run-1", 2, "model.delta", Map.of("text", "B")));
        assertThat(delivered).hasSize(1);
    }

    @Test
    void terminalRunEventsCloseLiveSubscriptionExactlyOnce() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        RecordingUi ui = installUi();
        Component owner = new com.vaadin.flow.component.html.Div();
        ui.add(owner);
        List<RunEventDto> delivered = new ArrayList<>();

        ConsoleLiveRunEventSubscriber subscriber = new ConsoleLiveRunEventSubscriber(fanout);
        subscriber.subscribe(owner, "run-1", delivered::add);

        fanout.publish(domainEvent("event-1", "session-1", "run-1", 1, "run.completed", Map.of()));
        subscriber.close();

        assertThat(ui.accessCalls()).isEqualTo(1);
        assertThat(delivered).extracting(RunEventDto::type).containsExactly("run.completed");
        assertThat(fanout.subscriberCount("run-1")).isZero();
    }

    @Test
    void planSubmissionBeginsPendingAssistantBubbleAfterRunIdentityExistsBeforeAnyDelta() {
        RecordingBridge bridge = new RecordingBridge(List.of());
        ConsoleView view = liveView(bridge, new SseRunEventFanout());

        view.planChatSubmission("hello streaming");

        List<Component> assistants = primaryAssistantBubbles(view);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getAttribute("data-session-id")).isEqualTo("session-live");
        assertThat(assistants.getFirst().getElement().getAttribute("data-run-id")).isEqualTo("run-live");
        assertThat(assistants.getFirst().getElement().getAttribute("data-stream-state")).isEqualTo("pending");
        assertThat(bridge.listEventAfterSequences()).containsExactly(0L);
    }

    @Test
    void consoleExposesPushOrPollingFallbackStreamMode() {
        ConsoleView push = liveView(new RecordingBridge(List.of()), new SseRunEventFanout());
        ConsoleView fallback = new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()), new RecordingBridge(List.of()), new RunEventRenderer());

        assertThat(push.getElement().getAttribute("data-stream-mode")).isEqualTo("push");
        assertThat(fallback.getElement().getAttribute("data-stream-mode")).isEqualTo("polling-fallback");
    }

    @Test
    void replayedEventsApplyBeforeLiveSubscriptionAcceptsLaterEvents() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        ConsoleView view = liveView(new RecordingBridge(List.of(
                dto("replay-1", 1, "session-live", "run-live", "model.delta", Map.of("text", "A")))), fanout);

        view.planChatSubmission("hello streaming");
        fanout.publish(domainEvent("live-2", "session-live", "run-live", 2, "model.delta", Map.of("text", "B")));

        assertThat(view.chatPanel().messages()).containsExactly("hello streaming", "AB");
    }

    @Test
    void liveFanoutDeltasAppendToExistingPendingAssistantBubbleNotGenericRows() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        ConsoleView view = liveView(new RecordingBridge(List.of()), fanout);

        view.planChatSubmission("hello streaming");
        fanout.publish(domainEvent("live-1", "session-live", "run-live", 1, "model.delta", Map.of("text", "streamed")));

        List<Component> assistants = primaryAssistantBubbles(view);
        assertThat(assistants).hasSize(1);
        assertThat(assistants.getFirst().getElement().getTextRecursively()).isEqualTo("streamed");
        assertThat(view.chatPanel().messages()).containsExactly("hello streaming", "streamed");
    }

    @Test
    void fallbackRefreshUsesReducerDedupeStateAfterLiveDeliveryWithoutDuplicatingText() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        RecordingBridge bridge = new RecordingBridge(List.of(
                dto("live-1", 1, "session-live", "run-live", "model.delta", Map.of("text", "streamed"))));
        ConsoleView view = liveView(bridge, fanout);

        view.planChatSubmission("hello streaming");
        fanout.publish(domainEvent("live-1", "session-live", "run-live", 1, "model.delta", Map.of("text", "streamed")));
        int refreshed = view.refreshActiveRunEvents();

        assertThat(refreshed).isZero();
        assertThat(primaryAssistantBubbles(view)).hasSize(1);
        assertThat(view.chatPanel().messages()).containsExactly("hello streaming", "streamed");
    }

    @Test
    void secondaryLiveEventsRenderCompactCardsThroughRunEventRenderer() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        ConsoleView view = liveView(new RecordingBridge(List.of()), fanout);

        view.planChatSubmission("hello streaming");
        fanout.publish(domainEvent("tool-1", "session-live", "run-live", 1, "tool.started",
                Map.of("status", "STARTED", "toolName", "lookup")));

        assertThat(primaryAssistantBubbles(view)).hasSize(1);
        assertThat(view.chatPanel().messages()).containsExactly("hello streaming");
        assertThat(view.chatPanel().componentCount()).isEqualTo(1);
    }

    private static RecordingUi installUi() {
        RecordingUi ui = new RecordingUi();
        UI.setCurrent(ui);
        return ui;
    }

    private static ConsoleView liveView(RecordingBridge bridge, SseRunEventFanout fanout) {
        RecordingUi ui = installUi();
        ConsoleView view = new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()),
                bridge, new RunEventRenderer(), new ConsoleLiveRunEventSubscriber(fanout));
        ui.add(view);
        return view;
    }

    private static List<Component> primaryAssistantBubbles(Component root) {
        return descendants(root.getElement())
                .filter(element -> "assistant".equals(element.getAttribute("data-message-role")))
                .filter(element -> "primary-bubble".equals(element.getAttribute("data-message-kind")))
                .map(element -> element.getComponent().orElseThrow())
                .toList();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static RunEventDto dto(String eventId, long sequence, String sessionId, String runId, String type, Map<String, Object> payload) {
        return new RunEventDto(eventId, "tenant-1", "user-1", sessionId, runId, "step-1", "workspace-1",
                sequence, Instant.parse("2026-06-01T00:00:00Z"), type, "trace-1", "correlation-1", null,
                "USER", null, "schema", 1, payload);
    }

    private static io.github.pi_java.agent.domain.event.RunEvent domainEvent(
            String eventId,
            String sessionId,
            String runId,
            long sequence,
            String type,
            Map<String, Object> payload) {
        return new io.github.pi_java.agent.domain.event.RunEvent(
                eventId,
                new io.github.pi_java.agent.domain.common.PlatformIds.TenantId("tenant-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.UserId("user-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.SessionId(sessionId),
                new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                new io.github.pi_java.agent.domain.common.PlatformIds.StepId("step-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId("workspace-1"),
                sequence,
                Instant.parse("2026-06-01T00:00:00Z"),
                runEventType(type),
                new io.github.pi_java.agent.domain.common.PlatformIds.TraceId("00000000000000000000000000000001"),
                new io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId("correlation-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.CausationId("cause-1"),
                runEventPayload(type, payload),
                io.github.pi_java.agent.domain.event.EventVisibility.USER,
                new io.github.pi_java.agent.domain.event.RedactionMetadata(false, false, java.util.Set.of(), "test"));
    }

    private static io.github.pi_java.agent.domain.event.RunEventType runEventType(String type) {
        return switch (type) {
            case "model.delta" -> io.github.pi_java.agent.domain.event.RunEventType.MODEL_DELTA;
            case "run.completed" -> io.github.pi_java.agent.domain.event.RunEventType.RUN_COMPLETED;
            default -> io.github.pi_java.agent.domain.event.RunEventType.RUN_STARTED;
        };
    }

    private static io.github.pi_java.agent.domain.event.RunEventPayload runEventPayload(String type, Map<String, Object> payload) {
        return new io.github.pi_java.agent.domain.event.RunEventPayload.ExtensionPayload("test.lifecycle", "1", payload);
    }

    private static final class RecordingUi extends UI {
        private int accessCalls;

        @Override
        public Future<Void> access(Command command) {
            accessCalls++;
            command.execute();
            return CompletableFuture.completedFuture(null);
        }

        private int accessCalls() {
            return accessCalls;
        }
    }

    private static final class RecordingBridge implements ConsoleRunExecutionBridge {
        private final List<RunEventDto> replayEvents;
        private final List<Long> listEventAfterSequences = new ArrayList<>();

        private RecordingBridge(List<RunEventDto> replayEvents) {
            this.replayEvents = replayEvents;
        }

        @Override
        public SessionResponse createSession() {
            return new SessionResponse("tenant", "user", "session-live", "workspace", null, "ACTIVE", now(), now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            return new RunResponse("tenant", "user", sessionId, "run-live", "workspace", "QUEUED", "trace", "correlation", now(), now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            listEventAfterSequences.add(afterSequence);
            long next = replayEvents.stream().mapToLong(RunEventDto::sequence).max().orElse(afterSequence);
            return new EventHistoryResponse(sessionId, runId, replayEvents, afterSequence, next, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "CANCELLED", true, now(), "trace", "correlation");
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            return new PageResponse<>(List.of(), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
        }

        private List<Long> listEventAfterSequences() {
            return listEventAfterSequences;
        }

        private static Instant now() {
            return Instant.parse("2026-06-01T00:00:00Z");
        }
    }
}
