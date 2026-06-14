package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.adapter.web.sse.SseSubscription;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload.ExtensionPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RunSseIntegrationTest {

    @Test
    void fanoutSubscribeReturnsSubscriberId() {
        SseRunEventFanout fanout = new SseRunEventFanout();

        SseSubscription subscription = fanout.subscribe("run-1", ignored -> { });

        assertThat(subscription.runId()).isEqualTo("run-1");
        assertThat(subscription.subscriberId()).isNotBlank();
    }

    @Test
    void fanoutUnsubscribeRemovesSubscriber() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        List<RunEventDto> received = new ArrayList<>();
        SseSubscription subscription = fanout.subscribe("run-1", received::add);

        fanout.unsubscribe("run-1", subscription.subscriberId());
        fanout.publish(event("run-1", 1L));

        assertThat(received).isEmpty();
    }

    @Test
    void fanoutRemovesSubscriberOnSendFailure() {
        SseRunEventFanout fanout = new SseRunEventFanout();
        AtomicInteger failingAttempts = new AtomicInteger();
        List<RunEventDto> survivor = new ArrayList<>();
        fanout.subscribe("run-1", ignored -> {
            failingAttempts.incrementAndGet();
            throw new IllegalStateException("broken subscriber");
        });
        fanout.subscribe("run-1", survivor::add);

        fanout.publish(event("run-1", 1L));
        fanout.publish(event("run-1", 2L));

        assertThat(failingAttempts).hasValue(1);
        assertThat(survivor).extracting(RunEventDto::sequence).containsExactly(1L, 2L);
    }

    private static RunEvent event(String runId, long sequence) {
        return new RunEvent(
                "event-" + sequence,
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-1"),
                new RunId(runId),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                sequence,
                Instant.parse("2026-06-14T00:00:00Z"),
                RunEventType.MODEL_DELTA,
                new TraceId("trace-1"),
                new CorrelationId("corr-1"),
                new CausationId("cause-1"),
                new ExtensionPayload("model.delta.schema", "1", Map.of("text", "hello")),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "policy-1"));
    }
}
