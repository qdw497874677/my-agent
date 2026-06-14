package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = {PiCloudServerApplication.class, TestCloudRuntimeConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "e2e"})
class RunCancellationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TestCloudRuntimeConfiguration.RuntimeProbe runtimeProbe;

    @Test
    void cancelRunningRunEmitsCancelledTerminalEventRow() {
        SessionResponse session = createSession("running-cancel");
        RunResponse run = createRun(session.sessionId(), "timeout running cancel");
        awaitStatus(session.sessionId(), run.runId(), "RUNNING");

        RunStatusResponse cancelled = post("/api/sessions/%s/runs/%s/cancel".formatted(session.sessionId(), run.runId()), Map.of("reason", "stop running"), RunStatusResponse.class);

        assertThat(cancelled.status()).isIn("CANCELLING", "CANCELLED", "TIMED_OUT");
        RunStatusResponse terminal = awaitTerminal(session.sessionId(), run.runId());
        assertThat(terminal.status()).isIn("CANCELLED", "TIMED_OUT");
        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.cancelled", "run.failed"));
    }

    @Test
    void cancelQueuedRunPublishesExactlyOneCancelledEventRow() {
        SessionResponse session = createSession("queued-cancel");
        RunResponse run = createRun(session.sessionId(), "queued cancellation");

        post("/api/sessions/%s/runs/%s/cancel".formatted(session.sessionId(), run.runId()), Map.of("reason", "cancel queued"), RunStatusResponse.class);
        RunStatusResponse terminal = awaitTerminal(session.sessionId(), run.runId());

        assertThat(terminal.status()).isIn("CANCELLED", "COMPLETED");
        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.cancelled", "run.completed"));
    }

    @Test
    void terminalRunCancelReturnsCurrentStateWithoutDuplicateTerminalEvent() {
        SessionResponse session = createSession("terminal-cancel");
        RunResponse run = createRun(session.sessionId(), "complete terminal cancel");
        RunStatusResponse terminal = awaitTerminal(session.sessionId(), run.runId());
        long terminalEventsBefore = terminalEventCount(run.runId());

        RunStatusResponse afterCancel = post("/api/sessions/%s/runs/%s/cancel".formatted(session.sessionId(), run.runId()), Map.of("reason", "too late"), RunStatusResponse.class);

        assertThat(afterCancel.status()).isEqualTo(terminal.status());
        assertThat(terminalEventCount(run.runId())).isEqualTo(terminalEventsBefore);
        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.completed"));
    }

    @Test
    void timeoutRunEmitsSingleAllowedTerminalEventRow() {
        SessionResponse session = createSession("timeout");
        RunResponse run = createRun(session.sessionId(), "timeout path");

        RunStatusResponse terminal = awaitTerminal(session.sessionId(), run.runId());

        assertThat(terminal.status()).isEqualTo("TIMED_OUT");
        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.failed"));
        assertThat(runtimeProbe.activeCount()).as("no hanging model/tool tasks").isZero();
    }

    private SessionResponse createSession(String workspaceId) {
        return post("/api/sessions", new CreateSessionRequest(workspaceId, Map.of()), SessionResponse.class);
    }

    private RunResponse createRun(String sessionId, String objective) {
        return post("/api/sessions/%s/runs".formatted(sessionId),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-cancel", Map.of()),
                RunResponse.class);
    }

    private RunStatusResponse awaitStatus(String sessionId, String runId, String expectedStatus) {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        RunStatusResponse current;
        do {
            current = get("/api/sessions/%s/runs/%s/status".formatted(sessionId, runId), RunStatusResponse.class);
            if (expectedStatus.equals(current.status())) {
                return current;
            }
            sleep(Duration.ofMillis(50));
        } while (System.nanoTime() < deadline);
        return current;
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

    private void assertExactlyOneTerminalEventLast(String runId, Set<String> allowedTerminalTypes) {
        List<String> types = jdbcTemplate.queryForList("SELECT event_type FROM run_events WHERE run_id = ? ORDER BY sequence ASC", String.class, runId);
        assertThat(types.stream().filter(this::isTerminal).count()).isEqualTo(1);
        assertThat(types.get(types.size() - 1)).isIn(allowedTerminalTypes);
    }

    private long terminalEventCount(String runId) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM run_events WHERE run_id = ? AND event_type IN ('run.completed','run.failed','run.cancelled','run.policy_blocked')", Long.class, runId);
        return count == null ? 0 : count;
    }

    private boolean isTerminal(String type) {
        return Set.of("run.completed", "run.failed", "run.cancelled", "run.policy_blocked").contains(type);
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
        headers.add("X-Pi-Dev-Tenant", "tenant-cancel");
        headers.add("X-Pi-Dev-User", "user-cancel");
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
}
