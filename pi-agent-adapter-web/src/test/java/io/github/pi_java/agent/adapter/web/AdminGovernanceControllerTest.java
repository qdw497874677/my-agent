package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.GovernanceQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
class AdminGovernanceControllerTest {

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
    void overviewContainsRuntimeRegistryGovernanceAndFutureExtensionStatuses() {
        GovernanceOverviewResponse response = sampleOverview();

        assertThat(response.runtime().status()).isEqualTo("HEALTHY");
        assertThat(response.providers().status()).isEqualTo("HEALTHY");
        assertThat(response.toolRegistry().status()).isEqualTo("HEALTHY");
        assertThat(response.extensions().status()).isEqualTo("FUTURE_ENABLED");
        assertThat(response.mcp().status()).isEqualTo("UNCONFIGURED");
        assertThat(response.plugins().status()).isEqualTo("FUTURE_ENABLED");
        assertThat(response.policyDecisions()).extracting(PolicyDecisionSummaryDto::decision)
                .contains("ALLOW", "REQUIRE_APPROVAL");
        assertThat(response.audits()).extracting(AuditSummaryDto::action)
                .contains("tool.policy", "tool.approval");
    }

    @Test
    void adminGovernanceDtosArePlainRedactedClientRecords() throws Exception {
        Class<?>[] dtoTypes = {
                GovernanceOverviewResponse.class,
                GovernanceStatusDto.class,
                PolicyDecisionSummaryDto.class,
                AuditSummaryDto.class
        };
        for (Class<?> dtoType : dtoTypes) {
            assertThat(dtoType.isRecord()).as(dtoType.getName()).isTrue();
            for (RecordComponent component : dtoType.getRecordComponents()) {
                assertThat(component.getType().getName()).doesNotContain("io.github.pi_java.agent.domain");
            }
        }

        String json = objectMapper.writeValueAsString(sampleOverview());
        assertThat(json)
                .contains("[REDACTED]")
                .doesNotContain("sk-test-secret")
                .doesNotContain("rawSecret")
                .doesNotContain("apiKey")
                .doesNotContain("password");

        Path clientAdminDir = Path.of(System.getProperty("user.dir")).getParent()
                .resolve("pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin");
        for (Path file : Files.list(clientAdminDir).filter(path -> path.toString().endsWith(".java")).toList()) {
            String source = Files.readString(file);
            assertThat(source)
                    .as(file.toString())
                    .doesNotContain("import io.github.pi_java.agent.domain")
                    .doesNotContain("import org.springframework")
                    .doesNotContain("import jakarta");
        }
    }

    @Test
    void authenticatedGovernanceEndpointsReturnReadOnlyDtos() throws Exception {
        when(governanceQueryService.overview(any())).thenReturn(sampleOverview());
        when(governanceQueryService.policyDecisions(any())).thenReturn(sampleOverview().policyDecisions());
        when(governanceQueryService.audits(any())).thenReturn(sampleOverview().audits());

        mockMvc.perform(get("/api/admin/governance/overview")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .header("X-Correlation-ID", "corr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtime.status").value("HEALTHY"))
                .andExpect(jsonPath("$.providers.count").value(1))
                .andExpect(jsonPath("$.toolRegistry.count").value(2))
                .andExpect(jsonPath("$.extensions.status").value("FUTURE_ENABLED"))
                .andExpect(jsonPath("$.mcp.status").value("UNCONFIGURED"))
                .andExpect(jsonPath("$.plugins.status").value("FUTURE_ENABLED"))
                .andExpect(jsonPath("$.policyDecisions[0].decision").value("ALLOW"))
                .andExpect(jsonPath("$.audits[0].action").value("tool.policy"));

        mockMvc.perform(get("/api/admin/governance/policy-decisions")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].decision").value("REQUIRE_APPROVAL"));

        mockMvc.perform(get("/api/admin/governance/audits")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].action").value("tool.approval"));

        verify(governanceQueryService).overview(any());
        verify(governanceQueryService).policyDecisions(any());
        verify(governanceQueryService).audits(any());
    }

    @Test
    void adminGovernanceControllerIsInspectOnlyInPhaseFive() throws Exception {
        for (String path : List.of("/api/admin/governance", "/api/admin/governance/overview", "/api/admin/governance/policy-decisions", "/api/admin/governance/audits")) {
            mockMvc.perform(post(path).header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(put(path).header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(patch(path).header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(delete(path).header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
        }

        Class<?> controllerClass = Class.forName("io.github.pi_java.agent.adapter.web.controller.AdminGovernanceController");
        for (Method method : controllerClass.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("io.github.pi_java.agent.domain");
            assertThat(method.getDeclaredAnnotations())
                    .noneMatch(annotation -> annotation.annotationType().getSimpleName().matches("PostMapping|PutMapping|PatchMapping|DeleteMapping"));
        }
    }

    private static GovernanceOverviewResponse sampleOverview() {
        List<PolicyDecisionSummaryDto> policies = List.of(
                new PolicyDecisionSummaryDto("decision-1", "ALLOW", "safe read-only tool", "builtin.read_info",
                        "call-1", "session-1", "run-1", Instant.parse("2026-06-15T00:00:00Z"), Map.of("summary", "[REDACTED]")),
                new PolicyDecisionSummaryDto("decision-2", "REQUIRE_APPROVAL", "workspace write requires approval", "builtin.write_resource",
                        "call-2", "session-1", "run-2", Instant.parse("2026-06-15T00:01:00Z"), Map.of("path", "notes.md")));
        List<AuditSummaryDto> audits = List.of(
                new AuditSummaryDto("audit-1", "tool.policy", "tool", "builtin.read_info", "session-1", "run-1",
                        Instant.parse("2026-06-15T00:00:01Z"), Map.of("decision", "ALLOW", "secret", "[REDACTED]")),
                new AuditSummaryDto("audit-2", "tool.approval", "tool", "builtin.write_resource", "session-1", "run-2",
                        Instant.parse("2026-06-15T00:01:01Z"), Map.of("decision", "APPROVED")));
        return new GovernanceOverviewResponse(
                new GovernanceStatusDto("runtime", "HEALTHY", "Runtime query API available", 1, Map.of("mode", "cloud")),
                new GovernanceStatusDto("providers", "HEALTHY", "Model provider registry available", 1, Map.of("defaultProvider", "openai-compatible")),
                new GovernanceStatusDto("toolRegistry", "HEALTHY", "Governed tool registry available", 2, Map.of("surface", "read-only")),
                new GovernanceStatusDto("extensions", "FUTURE_ENABLED", "SPI and Spring extension governance arrives in Phase 6", 0, Map.of("mutable", "false")),
                new GovernanceStatusDto("mcp", "UNCONFIGURED", "Remote MCP governance arrives in Phase 7", 0, Map.of("mutable", "false")),
                new GovernanceStatusDto("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8", 0, Map.of("mutable", "false")),
                policies,
                audits,
                Instant.parse("2026-06-15T00:02:00Z"));
    }
}
