package io.github.pi_java.agent.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.GovernanceQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.client.admin.OperationMetricDto;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.time.Instant;
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
class AdminOperationsControllerTest {

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
    private io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger runActivationTrigger;
    @MockBean
    private ToolRegistryQueryService toolRegistryQueryService;
    @MockBean
    private AgentCatalogQueryService agentCatalogQueryService;
    @MockBean
    private GovernanceQueryService governanceQueryService;
    @MockBean
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void authenticatedAdminCanFetchOperationsSummary() throws Exception {
        when(governanceQueryService.operations(any())).thenReturn(new OperationsSummaryResponse(
                List.of(new OperationMetricDto("runs", "pi.run.events.total", "HEALTHY", 2.0, "count", Map.of("status", "success"))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-06-19T00:00:00Z")));

        mockMvc.perform(get("/api/admin/governance/operations")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs[0].area").value("runs"))
                .andExpect(jsonPath("$.runs[0].name").value("pi.run.events.total"))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.generatedAt").value("2026-06-19T00:00:00Z"));

        verify(governanceQueryService).operations(any());
    }
}
