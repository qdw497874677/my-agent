package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {PiCloudServerApplication.class, SecurityAndCorrelationIntegrationTest.TestApi.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndCorrelationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/test-context")
                        .header("X-Pi-Dev-Disable-Defaults", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devHeadersCreateTenantUserContext() throws Exception {
        mockMvc.perform(get("/api/test-context")
                        .header("X-Pi-Dev-Tenant", "tenant-a")
                        .header("X-Pi-Dev-User", "user-a")
                        .header("X-Correlation-ID", "corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "corr-123"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("tenant-a");
                    assertThat(body).contains("user-a");
                    assertThat(body).contains("corr-123");
                });
    }

    @Test
    void responseContainsCorrelationHeader() throws Exception {
        mockMvc.perform(get("/api/test-context"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestApi {

        @Bean
        TestContextController testContextController() {
            return new TestContextController();
        }
    }

    @RestController
    @RequestMapping("/api")
    static class TestContextController {

        @GetMapping("/test-context")
        ResponseEntity<Map<String, String>> context(Principal principal, HttpServletRequest request) {
            Authentication authentication = (Authentication) principal;
            Object user = authentication == null ? null : authentication.getPrincipal();
            return ResponseEntity.ok(Map.of(
                    "principal", String.valueOf(user),
                    "traceId", String.valueOf(request.getAttribute("pi.traceId")),
                    "correlationId", String.valueOf(request.getAttribute("pi.correlationId")),
                    "causationId", String.valueOf(request.getAttribute("pi.causationId"))));
        }
    }
}
