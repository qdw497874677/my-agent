package io.github.pi_java.agent.adapter.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, PluginGovernanceApiTest.FakePluginConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PluginGovernanceApiTest {
    private static final String FAKE_SECRET = "PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK";
    private static final String RAW_PATH = "/var/secret/plugins/fake-plugin.jar";

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
    void authenticatedPluginGovernanceEndpointReturnsStatusAndNoRawSecretOrPath() throws Exception {
        mockMvc.perform(get("/api/admin/governance/plugins")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .header("X-Correlation-ID", "corr-plugin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plugins[0].pluginId").value("fake-plugin"))
                .andExpect(jsonPath("$.plugins[0].sourceKind").value("PF4J_JAR"))
                .andExpect(jsonPath("$.plugins[0].capabilities[0].capabilityId").value("plugin.fake.read"))
                .andExpect(content().string(not(containsString(FAKE_SECRET))))
                .andExpect(content().string(not(containsString(RAW_PATH))));
    }

    @Test
    void pluginRefreshEndpointTriggersRediscoveryWithoutPluginCrud() throws Exception {
        mockMvc.perform(post("/api/admin/governance/plugins/refresh")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.operation").value("refresh"))
                .andExpect(jsonPath("$.status").value("REFRESH_REQUESTED"))
                .andExpect(content().string(not(containsString(FAKE_SECRET))));

        for (var builder : List.of(put("/api/admin/governance/plugins"), patch("/api/admin/governance/plugins"),
                delete("/api/admin/governance/plugins"))) {
            mockMvc.perform(builder.header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Test
    void pluginDisableAndQuarantineDelegateReasonAndReturnRedactedMutationStatus() throws Exception {
        mockMvc.perform(post("/api/admin/governance/plugins/fake-plugin/disable")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"disable\",\"reason\":\"contains secret=top at /var/secret/plugins/fake-plugin.jar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pluginId").value("fake-plugin"))
                .andExpect(jsonPath("$.operation").value("disable"))
                .andExpect(jsonPath("$.resultingLifecycleStatus").value("DISABLED"))
                .andExpect(content().string(not(containsString("secret=top"))))
                .andExpect(content().string(not(containsString(RAW_PATH))));

        mockMvc.perform(post("/api/admin/governance/plugins/fake-plugin/quarantine")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"quarantine\",\"reason\":\"operator isolation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pluginId").value("fake-plugin"))
                .andExpect(jsonPath("$.operation").value("quarantine"))
                .andExpect(jsonPath("$.resultingLifecycleStatus").value("QUARANTINED"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakePluginConfiguration {
        @Bean
        @Primary
        PluginGovernanceCatalog fakePluginGovernanceCatalog() {
            return new PluginGovernanceCatalog() {
                @Override
                public List<PluginSourceStatus> plugins() {
                    return List.of(new PluginSourceStatus("fake-plugin", "Fake Plugin", "1.0.0", "Pi Test",
                            "PF4J_JAR", "STARTED", true, "UP", "COMPATIBLE", 1, Map.of("USABLE", "1"),
                            "", "fake-plugin.jar", "", Instant.EPOCH,
                            List.of(new PluginCapabilityStatus("plugin.fake.read", "TOOL", "USABLE", "1.0.0",
                                    "fake-plugin", true, "COMPATIBLE", "UP", Map.of("hint", "redacted"))),
                            Map.of("metadataKeys", "safe,secretKey")));
                }

                @Override
                public PluginMutationStatus refresh() {
                    return new PluginMutationStatus(true, "", "refresh", "", "", "REFRESH_REQUESTED", "",
                            Map.of("mode", "manual"));
                }

                @Override
                public PluginMutationStatus disable(String pluginId, String actor, String reason) {
                    return mutation(pluginId, "disable", "DISABLED");
                }

                @Override
                public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
                    return mutation(pluginId, "quarantine", "QUARANTINED");
                }

                private PluginMutationStatus mutation(String pluginId, String operation, String resulting) {
                    return new PluginMutationStatus(true, pluginId, operation, "STARTED", resulting, resulting, "",
                            Map.of("actor", "admin-a", "reason", "<redacted>"));
                }
            };
        }
    }
}
