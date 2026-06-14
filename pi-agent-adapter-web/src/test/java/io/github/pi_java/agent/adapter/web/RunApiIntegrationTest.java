package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pi_java.agent.adapter.web.controller.RunController;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PiCloudServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void createRunRequiresAuthAndReturnsAccepted() throws Exception {
        CreateRunRequest request = new CreateRunRequest("agent-general", "task", Map.of("prompt", "hello"), "workspace-1", Map.of());
        RunResponse response = new RunResponse("tenant-a", "user-a", "session-1", "run-1", "workspace-1", "QUEUED", "trace-1", "corr-1", Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"));
        when(runCommandService.createRun(any(), eq("session-1"), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/session-1/runs")
                        .header("X-Pi-Dev-Disable-Defaults", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        verify(runActivationTrigger, never()).triggerAsync();

        mockMvc.perform(post("/api/sessions/session-1/runs")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .header("X-Correlation-ID", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<RequestContext> context = ArgumentCaptor.forClass(RequestContext.class);
        verify(runCommandService).createRun(context.capture(), eq("session-1"), eq(request));
        assertThat(context.getValue().tenantId()).isEqualTo("tenant-a");
        assertThat(context.getValue().userId()).isEqualTo("user-a");
        assertThat(context.getValue().correlationId()).isEqualTo("corr-1");
    }

    @Test
    void cancelRunDelegatesWithSessionAndRunId() throws Exception {
        CancelRunRequest request = new CancelRunRequest("user requested");
        RunStatusResponse response = new RunStatusResponse("session-1", "run-1", "CANCELLED", true, Instant.parse("2026-06-14T00:00:00Z"), "trace-1", "corr-1");
        when(runCommandService.cancelRun(any(), eq("session-1"), eq("run-1"), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/session-1/runs/run-1/cancel")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(runCommandService).cancelRun(any(), eq("session-1"), eq("run-1"), eq(request));
    }

    @Test
    void createRunInvokesActivationTriggerWithoutManualDispatcherCall() throws Exception {
        CreateRunRequest request = new CreateRunRequest("agent-general", "task", Map.of("prompt", "hello"), "workspace-1", Map.of());
        RunResponse response = new RunResponse("tenant-a", "user-a", "session-1", "run-1", "workspace-1", "QUEUED", "trace-1", "corr-1", Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"));
        when(runCommandService.createRun(any(), eq("session-1"), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/session-1/runs")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(runActivationTrigger).triggerAsync();
    }

    @Test
    void controllersDoNotReturnDomainRecords() {
        for (Method method : RunController.class.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName())
                    .doesNotContain("io.github.pi_java.agent.domain");
        }
    }
}
