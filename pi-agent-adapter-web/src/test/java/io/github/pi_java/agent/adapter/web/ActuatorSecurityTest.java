package io.github.pi_java.agent.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void healthRemainsPublic() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health"), String.class);
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void prometheusRequiresAuthentication() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Pi-Dev-Disable-Defaults", "true");
        ResponseEntity<String> response = restTemplate.exchange(url("/actuator/prometheus"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void prometheusAllowsAuthenticatedDevPrincipal() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Pi-Dev-Tenant", "tenant-a");
        headers.add("X-Pi-Dev-User", "user-a");
        ResponseEntity<String> response = restTemplate.exchange(url("/actuator/prometheus"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
