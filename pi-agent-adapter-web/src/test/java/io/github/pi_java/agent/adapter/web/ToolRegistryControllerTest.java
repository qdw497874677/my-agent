package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pi_java.agent.adapter.web.mapper.RunEventDtoMapper;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
import io.github.pi_java.agent.client.tool.ToolDescriptorDto;
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
import io.github.pi_java.agent.domain.event.RunEventPayload.ToolLifecyclePayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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
class ToolRegistryControllerTest {

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
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void getToolsReturnsCatalogMetadataWithoutExecutorOrSecrets() throws Exception {
        ToolCatalogResponse response = new ToolCatalogResponse(Lists.tools());
        when(toolRegistryQueryService.listTools(any())).thenReturn(response);

        String body = mockMvc.perform(get("/api/tools")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .header("X-Correlation-ID", "corr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools[0].id").value("builtin.read_info"))
                .andExpect(jsonPath("$.tools[0].inputSchema.document.type").value("object"))
                .andExpect(jsonPath("$.tools[0].provenance.sourceKind").value("BUILT_IN"))
                .andExpect(jsonPath("$.tools[0].riskLevel").value("LOW"))
                .andExpect(jsonPath("$.tools[0].sideEffect").value("READ_ONLY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(toolRegistryQueryService).listTools(any());
        assertThat(body)
                .doesNotContain("executor")
                .doesNotContain("ToolExecutorBinding")
                .doesNotContain("DefaultSecretResolver")
                .doesNotContain("rawSecret")
                .doesNotContain("sk-test-secret")
                .doesNotContain("apiKey");
    }

    @Test
    void toolRegistryControllerIsReadOnlyAndReturnsClientDtos() throws Exception {
        mockMvc.perform(post("/api/tools").header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "user-a"))
                .andExpect(status().isMethodNotAllowed());
        Class<?> controllerClass = Class.forName("io.github.pi_java.agent.adapter.web.controller.ToolRegistryController");
        for (Method method : controllerClass.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("io.github.pi_java.agent.domain");
            assertThat(method.getDeclaredAnnotations()).noneMatch(annotation -> annotation.annotationType().getSimpleName().matches("PostMapping|PutMapping|PatchMapping|DeleteMapping"));
        }
    }

    @Test
    void toolLifecyclePayloadMapsToStablePublicDtoWithRedactedSummaries() throws Exception {
        RunEvent event = new RunEvent(
                "event-1",
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-1"),
                new RunId("run-1"),
                new StepId("step-1"),
                new WorkspaceId("workspace-1"),
                7L,
                Instant.parse("2026-06-14T00:00:00Z"),
                RunEventType.TOOL_PREVIEW_GENERATED,
                new TraceId("trace-1"),
                new CorrelationId("corr-1"),
                new CausationId("cause-1"),
                new ToolLifecyclePayload(
                        "call-1",
                        "builtin.write_resource",
                        "1.0.0",
                        new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin", "builtin.write_resource", Map.of("executorClass", "secret.Executor")),
                        Map.of("path", "notes.md", "token", "[REDACTED]"),
                        Map.of("bytes", 12, "secret", "[REDACTED]"),
                        Optional.of(PolicyDecision.REQUIRE_APPROVAL),
                        Optional.of(ToolExecutionStatus.PREVIEW_ONLY),
                        Optional.of(new ProvisionPreview("preview-1", "write workspace resource", Set.of("workspace-write"), true, Map.of("rawSecret", "[REDACTED]"))),
                        Optional.empty()),
                EventVisibility.USER,
                new RedactionMetadata(true, true, Set.of("token", "secret"), "tool-redaction"));

        JsonNode json = objectMapper.valueToTree(RunEventDtoMapper.toDto(event));

        assertThat(json.get("type").asText()).isEqualTo("tool.preview_generated");
        assertThat(json.get("payloadSchema").asText()).isEqualTo("tool.lifecycle");
        assertThat(json.get("payloadVersion").asInt()).isEqualTo(1);
        assertThat(json.at("/payload/toolCallId").asText()).isEqualTo("call-1");
        assertThat(json.at("/payload/toolId").asText()).isEqualTo("builtin.write_resource");
        assertThat(json.at("/payload/provenance/sourceKind").asText()).isEqualTo("BUILT_IN");
        assertThat(json.at("/payload/provenance/metadata/executorClass").isMissingNode()).isTrue();
        assertThat(json.at("/payload/policyDecision").asText()).isEqualTo("REQUIRE_APPROVAL");
        assertThat(json.at("/payload/executionStatus").asText()).isEqualTo("PREVIEW_ONLY");
        assertThat(json.at("/redaction/redacted").asBoolean()).isTrue();
        assertThat(json.toString())
                .doesNotContain("secret.Executor")
                .doesNotContain("token-value")
                .doesNotContain("rawSecret")
                .doesNotContain("sk-test-secret");
    }

    private static final class Lists {
        private static java.util.List<ToolDescriptorDto> tools() {
            return java.util.List.of(new ToolDescriptorDto(
                    "builtin.read_info",
                    "Read built-in information",
                    "Returns injected read-only platform information.",
                    new ToolDescriptorDto.SchemaDto("json-schema-2020-12", Map.of("type", "object"), Set.of("secret"), 4096),
                    Optional.empty(),
                    new ToolDescriptorDto.ProvenanceDto("BUILT_IN", "builtin", "builtin.read_info", Map.of("category", "info")),
                    "1.0.0",
                    Set.of("tool:read"),
                    "LOW",
                    "READ_ONLY",
                    Duration.ofSeconds(2),
                    Map.of("category", "info")));
        }
    }
}
