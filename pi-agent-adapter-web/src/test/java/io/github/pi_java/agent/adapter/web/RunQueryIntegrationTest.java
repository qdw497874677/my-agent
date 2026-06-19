package io.github.pi_java.agent.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.mapper.RunEventDtoMapper;
import io.github.pi_java.agent.adapter.web.controller.RunController;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
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
import java.util.Set;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionCommandService sessionCommandService;

    @MockBean
    private SessionQueryService sessionQueryService;

    @MockBean
    private RunCommandService runCommandService;

    @MockBean
    private RunQueryService runQueryService;

    @MockBean
    private RunController.RunActivationTrigger runActivationTrigger;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void queryEndpointsExposeCloud03Resources() throws Exception {
        when(runQueryService.getRunStatus(any(), eq("session-1"), eq("run-1")))
                .thenReturn(new RunStatusResponse("session-1", "run-1", "RUNNING", false, Instant.parse("2026-06-14T00:00:00Z"), "trace-1", "corr-1"));
        when(runQueryService.listSteps(any(), eq("session-1"), eq("run-1"), eq(500)))
                .thenReturn(new PageResponse<>(List.of(Map.of("stepId", "step-1")), 500, null, null, false));
        when(runQueryService.listMessages(any(), eq("session-1"), eq("run-1"), eq(500)))
                .thenReturn(new PageResponse<>(List.of(Map.of("messageId", "message-1")), 500, null, null, false));
        when(runQueryService.listToolCalls(any(), eq("session-1"), eq("run-1"), eq(500)))
                .thenReturn(new PageResponse<>(List.of(Map.of("toolCallId", "tool-1")), 500, null, null, false));
        when(runQueryService.getRunResult(any(), eq("session-1"), eq("run-1")))
                .thenReturn(new RunResultResponse("run-1", "COMPLETED", Map.of("text", "done"), Map.of()));

        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stepId").value("step-1"));
        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].messageId").value("message-1"));
        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/tool-calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].toolCallId").value("tool-1"));
        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminalResult.text").value("done"));
    }

    @Test
    void eventHistoryUsesAfterSequenceAndLimit() throws Exception {
        when(runQueryService.listEvents(any(), eq("session-1"), eq("run-1"), eq(7L), eq(25)))
                .thenReturn(new EventHistoryResponse("session-1", "run-1", List.of(), 7L, 8L, false));

        mockMvc.perform(get("/api/sessions/session-1/runs/run-1/events")
                        .param("afterSequence", "7")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afterSequence").value(7))
                .andExpect(jsonPath("$.nextAfterSequence").value(8));

        verify(runQueryService).listEvents(any(), eq("session-1"), eq("run-1"), eq(7L), eq(25));
    }

    @Test
    void mapperPreservesWireNameSequenceCorrelationRedactionAndPayloadIdentity() {
        RunEvent event = new RunEvent(
                "event-1",
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-1"),
                new RunId("run-1"),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                42L,
                Instant.parse("2026-06-14T00:00:00Z"),
                RunEventType.MODEL_DELTA,
                new TraceId("0123456789abcdef0123456789abcdef"),
                new CorrelationId("corr-1"),
                new CausationId("cause-1"),
                new ExtensionPayload("model.delta.schema", "2", Map.of("text", "hello")),
                EventVisibility.USER,
                new RedactionMetadata(true, true, Set.of("secret"), "policy-1"));

        RunEventDto dto = RunEventDtoMapper.toDto(event);

        assertThat(dto.type()).isEqualTo("model.delta");
        assertThat(dto.sequence()).isEqualTo(42L);
        assertThat(dto.correlationId()).isEqualTo("corr-1");
        assertThat(dto.causationId()).isEqualTo("cause-1");
        assertThat(dto.redaction().redacted()).isTrue();
        assertThat(dto.redaction().redactedFields()).containsExactly("secret");
        assertThat(dto.redaction().policy()).isEqualTo("policy-1");
        assertThat(dto.payloadSchema()).isEqualTo("model.delta.schema");
        assertThat(dto.payloadVersion()).isEqualTo(2);
        assertThat(dto.payload()).containsEntry("text", "hello");
    }
}
