package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.adapter.web.sse.SseSubscription;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
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
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunSseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseRunEventFanout fanout;

    @MockBean
    private SessionCommandService sessionCommandService;

    @MockBean
    private SessionQueryService sessionQueryService;

    @MockBean
    private RunCommandService runCommandService;

    @MockBean
    private RunQueryService runQueryService;

    @MockBean
    private io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger runActivationTrigger;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void fanoutSubscribeReturnsSubscriberId() {
        SseSubscription subscription = fanout.subscribe("run-1", ignored -> { });

        assertThat(subscription.runId()).isEqualTo("run-1");
        assertThat(subscription.subscriberId()).isNotBlank();
    }

    @Test
    void fanoutUnsubscribeRemovesSubscriber() {
        List<RunEventDto> received = new ArrayList<>();
        SseSubscription subscription = fanout.subscribe("run-1", received::add);

        fanout.unsubscribe("run-1", subscription.subscriberId());
        fanout.publish(event("run-1", 1L));

        assertThat(received).isEmpty();
    }

    @Test
    void fanoutRemovesSubscriberOnSendFailure() {
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

    @Test
    void streamReplaysPersistedEventsBeforeLiveEvents() throws Exception {
        when(runQueryService.listEvents(any(), eq("session-1"), eq("run-stream-1"), eq(0L), eq(500)))
                .thenReturn(history("run-stream-1", 1L, 2L, false));

        MvcResult result = mockMvc.perform(get("/api/sessions/session-1/runs/run-stream-1/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("id:1", "id:2", "event:model.delta");
        assertThat(body.indexOf("id:1")).isLessThan(body.indexOf("id:2"));
        assertThat(fanout.subscriberCount("run-stream-1")).isEqualTo(1);
    }

    @Test
    void lastEventIdOverridesAfterSequence() throws Exception {
        when(runQueryService.listEvents(any(), eq("session-1"), eq("run-stream-2"), eq(9L), eq(500)))
                .thenReturn(history("run-stream-2", 10L, false));

        mockMvc.perform(get("/api/sessions/session-1/runs/run-stream-2/stream")
                        .param("afterSequence", "3")
                        .header("Last-Event-ID", "9"))
                .andExpect(request().asyncStarted());

        verify(runQueryService).listEvents(any(), eq("session-1"), eq("run-stream-2"), eq(9L), eq(500));
    }

    @Test
    void sseEventIdIsRunSequence() throws Exception {
        when(runQueryService.listEvents(any(), eq("session-1"), eq("run-stream-3"), eq(0L), eq(500)))
                .thenReturn(history("run-stream-3", 42L, true));

        MvcResult result = mockMvc.perform(get("/api/sessions/session-1/runs/run-stream-3/stream"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("id:42");
    }

    @Test
    void streamCleansSubscriberOnTimeoutErrorAndCompletion() throws Exception {
        when(runQueryService.listEvents(any(), eq("session-1"), eq("run-stream-4"), eq(0L), eq(500)))
                .thenReturn(history("run-stream-4", 1L, false));

        mockMvc.perform(get("/api/sessions/session-1/runs/run-stream-4/stream"))
                .andExpect(request().asyncStarted());
        assertThat(fanout.subscriberCount("run-stream-4")).isEqualTo(1);

        fanout.publish(event("run-stream-4", 2L, RunEventType.RUN_COMPLETED));

        assertThat(fanout.subscriberCount("run-stream-4")).isZero();
    }

    private static RunEvent event(String runId, long sequence) {
        return event(runId, sequence, RunEventType.MODEL_DELTA);
    }

    private static RunEvent event(String runId, long sequence, RunEventType type) {
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
                type,
                new TraceId("0123456789abcdef0123456789abcdef"),
                new CorrelationId("corr-1"),
                new CausationId("cause-1"),
                new ExtensionPayload("model.delta.schema", "1", Map.of("text", "hello")),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "policy-1"));
    }

    private static EventHistoryResponse history(String runId, long sequence, boolean terminal) {
        return history(runId, List.of(sequence), terminal);
    }

    private static EventHistoryResponse history(String runId, long firstSequence, long secondSequence, boolean terminal) {
        return history(runId, List.of(firstSequence, secondSequence), terminal);
    }

    private static EventHistoryResponse history(String runId, List<Long> sequences, boolean terminal) {
        List<RunEventDto> events = sequences.stream()
                .map(sequence -> io.github.pi_java.agent.adapter.web.mapper.RunEventDtoMapper.toDto(event(runId, sequence)))
                .toList();
        long after = sequences.isEmpty() ? 0L : sequences.get(0) - 1L;
        long next = sequences.isEmpty() ? after : sequences.get(sequences.size() - 1);
        return new EventHistoryResponse("session-1", runId, events, after, next, terminal);
    }
}
