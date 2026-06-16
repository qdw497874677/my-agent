package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = {PiCloudServerApplication.class, McpGovernedToolE2ETest.McpE2EConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class McpSecurityRedactionE2ETest {
    private static final String FAKE_SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void rawMcpSecretIsAbsentFromRestEventsAuditAdminAndNormalizedErrors() {
        RunOutcome outcome = runWithObjective("mcp auth failure " + FAKE_SECRET);

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertNoSecret(outcome.detail());
        assertNoSecret(outcome.events());
        assertNoSecret(stores.listByRun(outcome.run().runId(), 0, 500));
        assertNoSecret(stores.auditsForRun(outcome.run().runId()));
        assertNoSecret(get("/api/admin/governance/mcp", String.class));
        assertThat(outcome.events().toString()).contains("MCP_AUTH_FAILED").containsIgnoringCase("redacted");
    }

    @Test
    void mcpUiAndCatalogResponsesExposeCredentialRefsAndSummariesOnly() {
        String catalog = get("/api/tools", String.class);
        String admin = get("/api/admin/governance/mcp", String.class);

        assertThat(catalog).contains("mcp.fake.search", "mcp.remote").doesNotContain(FAKE_SECRET, "api_key", "password");
        assertThat(admin).contains("server-down").doesNotContain(FAKE_SECRET, "bearer=", "token=");
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-mcp-redaction", Map.of()), SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-mcp-redaction", Map.of()),
                RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        RunDetailResponse detail = get("/api/sessions/%s/runs/%s".formatted(session.sessionId(), run.runId()), RunDetailResponse.class);
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()),
                EventHistoryResponse.class);
        return new RunOutcome(session, run, status, detail, events);
    }

    private void assertNoSecret(Object value) {
        assertThat(value.toString()).doesNotContain(FAKE_SECRET);
    }

    private RunStatusResponse awaitTerminal(String sessionId, String runId) {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        RunStatusResponse current;
        do {
            current = get("/api/sessions/%s/runs/%s/status".formatted(sessionId, runId), RunStatusResponse.class);
            if (current.terminal()) {
                return current;
            }
            sleep(Duration.ofMillis(100));
        } while (System.nanoTime() < deadline);
        throw new AssertionError("run did not reach terminal state: " + runId);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private <T> T get(String path, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.add("X-Pi-Dev-Tenant", "tenant-mcp-redaction");
        headers.add("X-Pi-Dev-User", "user-mcp-redaction");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record RunOutcome(SessionResponse session, RunResponse run, RunStatusResponse status, RunDetailResponse detail,
                              EventHistoryResponse events) {
    }
}
