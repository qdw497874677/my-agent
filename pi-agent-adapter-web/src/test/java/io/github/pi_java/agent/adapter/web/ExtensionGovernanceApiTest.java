package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.extension.api.EventListenerExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, ExtensionGovernanceApiTest.ExtensionFixtureConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtensionGovernanceApiTest {

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
    void authenticatedExtensionGovernanceEndpointReturnsRealStarterCatalogData() throws Exception {
        mockMvc.perform(get("/api/admin/governance/extensions")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "admin-a")
                        .header("X-Correlation-ID", "corr-extension"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources[0].sourceId").value("test-spring-extension"))
                .andExpect(jsonPath("$.sources[0].kind").value("SPRING_BEAN"))
                .andExpect(jsonPath("$.sources[0].lifecycleStatus").value("USABLE"))
                .andExpect(jsonPath("$.sources[0].enabled").value(true))
                .andExpect(jsonPath("$.sources[0].compatibilityStatus").value("COMPATIBLE"))
                .andExpect(jsonPath("$.sources[0].healthStatus").value("UP"))
                .andExpect(jsonPath("$.sources[0].redactedError").value(""))
                .andExpect(jsonPath("$.sources[0].capabilities[0].capabilityId").value("listener.audit"))
                .andExpect(jsonPath("$.sources[0].capabilities[0].type").value("EVENT_LISTENER"))
                .andExpect(jsonPath("$.sources[0].capabilities[0].status").value("USABLE"))
                .andExpect(jsonPath("$.sources[0].capabilities[0].metadata['extension.sourceKind']").value("SPRING_BEAN"))
                .andExpect(jsonPath("$.sources[0].capabilities[0].metadata['error']").value("[REDACTED]"));
    }

    @Test
    void extensionGovernanceEndpointIsGetOnly() throws Exception {
        String path = "/api/admin/governance/extensions";
        for (var builder : List.of(post(path), put(path), patch(path), delete(path))) {
            mockMvc.perform(builder.header("X-Pi-Dev-Tenant", "tenant-a").header("X-Pi-Dev-User", "admin-a"))
                    .andExpect(status().isMethodNotAllowed());
        }

        Class<?> controllerClass = Class.forName("io.github.pi_java.agent.adapter.web.controller.AdminGovernanceController");
        Method extensions = controllerClass.getDeclaredMethod("extensions", java.security.Principal.class,
                jakarta.servlet.http.HttpServletRequest.class);
        assertThat(extensions.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().equals("GetMapping"));
        assertThat(extensions.getDeclaredAnnotations())
                .noneMatch(annotation -> annotation.annotationType().getSimpleName().matches("PostMapping|PutMapping|PatchMapping|DeleteMapping"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExtensionFixtureConfiguration {
        @Bean
        ExtensionSource fixtureExtensionSource() {
            return new ExtensionSource() {
                @Override
                public ExtensionMetadata metadata() {
                    return new ExtensionMetadata(
                            "test-spring-extension",
                            "Test Spring Extension",
                            "1.0.0",
                            "Pi Test",
                            io.github.pi_java.agent.extension.api.ExtensionApiVersion.parse("1.0.0"),
                            ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                            ExtensionLifecycleState.STARTED,
                            ExtensionHealth.up("ready"),
                            true,
                            Map.of("sourceKind", "SPRING_BEAN", "order", 10));
                }

                @Override
                public List<io.github.pi_java.agent.extension.api.ExtensionCapability> capabilities() {
                    return List.of(new EventListenerExtensionCapability(
                            "listener.audit",
                            Set.of("tool.lifecycle"),
                            Map.of("version", "1.0.0", "sourceKind", "SPRING_BEAN", "error", "[REDACTED]")));
                }
            };
        }
    }
}
