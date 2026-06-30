package io.github.pi_java.agent.adapter.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.adapter.web.ui.PiWebAppShell;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleLiveRunEventSubscriber;
import io.github.pi_java.agent.client.event.RunEventDto;
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

    private static RecordingUi installUi() {
        RecordingUi ui = new RecordingUi();
        UI.setCurrent(ui);
        return ui;
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
                new io.github.pi_java.agent.domain.event.RunEventType(type),
                new io.github.pi_java.agent.domain.common.PlatformIds.TraceId("trace-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId("correlation-1"),
                new io.github.pi_java.agent.domain.common.PlatformIds.CausationId("cause-1"),
                new io.github.pi_java.agent.domain.event.RunEventPayload("schema", 1, payload),
                io.github.pi_java.agent.domain.event.EventVisibility.USER,
                io.github.pi_java.agent.domain.event.RedactionMetadata.none());
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
}
