package io.github.pi_java.agent.adapter.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, McpGovernanceApiTest.FakeMcpConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpGovernanceApiTest {
    private static final String FAKE_SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";

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
    private AgentCatalogQueryService agentCatalogQueryService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void authenticatedMcpGovernanceEndpointReturnsConfiguredStatusAndNoSecretText() throws Exception {
        mockMvc.perform(post("/api/admin/governance/mcp/refresh")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/governance/mcp")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .header("X-Correlation-ID", "corr-mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].serverId").value("fake"))
                .andExpect(jsonPath("$.servers[0].connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.servers[0].discoveryStatus").value("DISCOVERED"))
                .andExpect(jsonPath("$.servers[0].tools[0].serverQualifiedToolId").value("mcp.fake.echo"))
                .andExpect(jsonPath("$.servers[0].tools[0].availabilityStatus").value("AVAILABLE"))
                .andExpect(content().string(not(containsString(FAKE_SECRET))));
    }

    @Test
    void governanceOverviewMcpStatusIsNoLongerFuturePlaceholder() throws Exception {
        mockMvc.perform(post("/api/admin/governance/mcp/refresh")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/governance")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mcp.status").value("HEALTHY"))
                .andExpect(jsonPath("$.mcp.metadata.surface").value("read-only"));
    }

    @Test
    void mcpRefreshEndpointTriggersRediscoveryWithoutConfigCrud() throws Exception {
        mockMvc.perform(post("/api/admin/governance/mcp/refresh")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshed").value(true))
                .andExpect(jsonPath("$.serverCount").value(1))
                .andExpect(jsonPath("$.refreshedServerCount").value(1))
                .andExpect(content().string(not(containsString(FAKE_SECRET))));

        for (var builder : List.of(put("/api/admin/governance/mcp"), patch("/api/admin/governance/mcp"), delete("/api/admin/governance/mcp"))) {
            mockMvc.perform(builder.header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeMcpConfiguration {
        @Bean
        @Primary
        McpServerRegistry fakeMcpServerRegistry(Clock clock) {
            McpServerProperties server = McpServerProperties.streamableHttp(
                    "fake", "Fake MCP", "https://mcp.fake.test", "/mcp",
                    McpAuthProperties.bearerTokenRef("env:FAKE_MCP_TOKEN"), Map.of("secret", FAKE_SECRET));
            return new McpServerRegistry(List.of(server), s -> new McpServerRegistry.DiscoveryClient() {
                @Override
                public List<McpSchema.Tool> listTools() {
                    return List.of(new McpSchema.Tool("echo", null, "Echo input",
                            new McpSchema.JsonSchema("object", Map.of(), List.of(), null, Map.of(), Map.of()),
                            null,
                            new McpSchema.ToolAnnotations("Echo", true, false, true, false, null),
                            Map.of()));
                }

                @Override
                public void close() {
                }
            }, clock);
        }
    }
}
