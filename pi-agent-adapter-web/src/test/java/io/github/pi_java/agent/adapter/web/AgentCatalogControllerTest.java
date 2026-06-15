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

import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
class AgentCatalogControllerTest {

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
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void catalogItemExposesRunDecisionMetadata() {
        AgentCatalogItemDto item = defaultAgent();

        assertThat(item.id()).isEqualTo("cloud-general-agent");
        assertThat(item.supportedInputModes()).containsExactlyInAnyOrder("CHAT", "TASK");
        assertThat(item.capabilities()).contains("tool-calling", "workspace");
        assertThat(item.modelRef().safeRef()).isEqualTo("openai-compatible:gpt-4.1-mini");
        assertThat(item.allowedToolIds()).contains("builtin.read_info", "builtin.write_resource");
        assertThat(item.allowedToolScopes()).contains("tool:read", "tool:workspace:write");
        assertThat(item.riskLabels()).contains("LOW", "MEDIUM");
        assertThat(item.sideEffectLabels()).contains("READ_ONLY", "WORKSPACE_WRITE");
        assertThat(item.entryActions()).extracting(AgentCatalogItemDto.EntryActionDto::actionType)
                .contains("CREATE_RUN");
    }

    @Test
    void agentCatalogDtosArePlainClientRecordsWithoutOuterLayerImports() throws Exception {
        Class<?> itemClass = Class.forName("io.github.pi_java.agent.client.agent.AgentCatalogItemDto");
        Class<?> responseClass = Class.forName("io.github.pi_java.agent.client.agent.AgentCatalogResponse");

        assertThat(itemClass.isRecord()).isTrue();
        assertThat(responseClass.isRecord()).isTrue();
        for (RecordComponent component : itemClass.getRecordComponents()) {
            assertThat(component.getType().getName()).doesNotContain("io.github.pi_java.agent.domain");
        }

        Path clientAgentDir = Path.of(System.getProperty("user.dir")).getParent()
                .resolve("pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent");
        for (Path file : Files.list(clientAgentDir).filter(path -> path.toString().endsWith(".java")).toList()) {
            String source = Files.readString(file);
            assertThat(source)
                    .as(file.toString())
                    .doesNotContain("import io.github.pi_java.agent.domain")
                    .doesNotContain("import org.springframework")
                    .doesNotContain("import jakarta");
        }
    }

    @Test
    void getAgentsReturnsDefaultRunnableCatalogForAuthenticatedCaller() throws Exception {
        when(agentCatalogQueryService.listAgents(any())).thenReturn(new AgentCatalogResponse(List.of(defaultAgent())));

        mockMvc.perform(get("/api/agents")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .header("X-Correlation-ID", "corr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agents[0].id").value("cloud-general-agent"))
                .andExpect(jsonPath("$.agents[0].modelRef.safeRef").value("openai-compatible:gpt-4.1-mini"))
                .andExpect(jsonPath("$.agents[0].supportedInputModes[0]").exists())
                .andExpect(jsonPath("$.agents[0].allowedToolIds[0]").exists())
                .andExpect(jsonPath("$.agents[0].riskLabels[0]").exists())
                .andExpect(jsonPath("$.agents[0].entryActions[0].actionType").value("CREATE_RUN"));

        verify(agentCatalogQueryService).listAgents(any());
    }

    @Test
    void agentCatalogControllerIsReadOnlyForPhaseFive() throws Exception {
        mockMvc.perform(post("/api/agents").header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "user-a"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/agents").header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "user-a"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/agents").header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "user-a"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/agents").header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "user-a"))
                .andExpect(status().isMethodNotAllowed());

        Class<?> controllerClass = Class.forName("io.github.pi_java.agent.adapter.web.controller.AgentCatalogController");
        for (Method method : controllerClass.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("io.github.pi_java.agent.domain");
            assertThat(method.getDeclaredAnnotations())
                    .noneMatch(annotation -> annotation.annotationType().getSimpleName().matches("PostMapping|PutMapping|PatchMapping|DeleteMapping"));
        }
    }

    private static AgentCatalogItemDto defaultAgent() {
        return new AgentCatalogItemDto(
                "cloud-general-agent",
                "Cloud General Agent",
                "General purpose governed cloud agent.",
                Set.of("CHAT", "TASK"),
                Set.of("chat", "tool-calling", "workspace"),
                new AgentCatalogItemDto.ModelRefDto("openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini"),
                Set.of("builtin.read_info", "builtin.write_resource"),
                Set.of("tool:read", "tool:workspace:write"),
                Set.of("LOW", "MEDIUM"),
                Set.of("READ_ONLY", "WORKSPACE_WRITE"),
                List.of(new AgentCatalogItemDto.EntryActionDto(
                        "start-chat", "Start chat", "CREATE_RUN", "CHAT", Map.of("inputType", "chat"))),
                Duration.ofSeconds(30),
                Map.of("defaultWorkspacePolicy", "default-workspace-policy"));
    }
}
