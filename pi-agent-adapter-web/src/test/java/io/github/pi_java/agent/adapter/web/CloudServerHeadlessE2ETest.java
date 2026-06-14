package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
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
class CloudServerHeadlessE2ETest {

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
    void successfulRunCreateStreamPersistQueryUsesAutomaticWorkerActivation() {
        SessionResponse session = createSession("workspace-success");
        RunResponse run = createRun(session.sessionId(), "complete normally");

        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        assertThat(status.status()).isEqualTo("COMPLETED");

        String stream = getText("/api/sessions/%s/runs/%s/stream".formatted(session.sessionId(), run.runId()), headers());
        assertThat(stream).contains("event:run.completed");

        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()), EventHistoryResponse.class);
        assertThat(events.events()).extracting(RunEventDto::type).contains("run.completed");
        assertThat(get("/api/sessions/%s/runs/%s/status".formatted(session.sessionId(), run.runId()), RunStatusResponse.class).terminal()).isTrue();
        assertThat(get("/api/sessions/%s/runs/%s/result".formatted(session.sessionId(), run.runId()), RunResultResponse.class).status()).isEqualTo("COMPLETED");
        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.completed"));
    }

    @Test
    void cancelAndTimeoutPathsTerminateWithPersistedTerminalEvents() {
        SessionResponse queuedSession = createSession("workspace-cancel");
        RunResponse queued = createRun(queuedSession.sessionId(), "queued cancel");
        post("/api/sessions/%s/runs/%s/cancel".formatted(queuedSession.sessionId(), queued.runId()), Map.of("reason", "cancel queued"), RunStatusResponse.class);
        awaitTerminal(queuedSession.sessionId(), queued.runId());
        assertExactlyOneTerminalEventLast(queued.runId(), Set.of("run.cancelled", "run.completed"));

        SessionResponse timeoutSession = createSession("workspace-timeout");
        RunResponse timeout = createRun(timeoutSession.sessionId(), "timeout please");
        RunStatusResponse timeoutStatus = awaitTerminal(timeoutSession.sessionId(), timeout.runId());
        assertThat(timeoutStatus.status()).isEqualTo("TIMED_OUT");
        assertExactlyOneTerminalEventLast(timeout.runId(), Set.of("run.failed"));
        assertNoHangingRuntimeTasks();
    }

    @Test
    void maxStepRunEmitsSingleTerminalEventAndDoesNotHang() {
        SessionResponse session = createSession("workspace-max-step");
        RunResponse run = createRun(session.sessionId(), "max-step RuntimeLimits should exceed exactly one terminal event and no hanging task");

        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        assertThat(status.status()).isEqualTo("FAILED");

        assertExactlyOneTerminalEventLast(run.runId(), Set.of("run.failed"));
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()), EventHistoryResponse.class);
        assertThat(events.events()).last().extracting(RunEventDto::type).isEqualTo("run.failed");
        assertNoHangingRuntimeTasks();
    }

    @Test
    void sseReconnectReplaysWithoutGaps() {
        SessionResponse session = createSession("workspace-replay");
        RunResponse run = createRun(session.sessionId(), "complete for replay");
        awaitTerminal(session.sessionId(), run.runId());

        String first = getText("/api/sessions/%s/runs/%s/stream".formatted(session.sessionId(), run.runId()), headers());
        long capturedSequence = sseIds(first).get(0);
        HttpHeaders reconnectHeaders = headers();
        reconnectHeaders.add("Last-Event-ID", Long.toString(capturedSequence));
        String replayed = getText("/api/sessions/%s/runs/%s/stream".formatted(session.sessionId(), run.runId()), reconnectHeaders);

        assertThat(sseIds(replayed)).allSatisfy(sequence -> assertThat(sequence).isGreaterThan(capturedSequence));
    }

    private SessionResponse createSession(String workspaceId) {
        return post("/api/sessions", new CreateSessionRequest(workspaceId, Map.of("source", "headless-e2e")), SessionResponse.class);
    }

    private RunResponse createRun(String sessionId, String objective) {
        return post("/api/sessions/%s/runs".formatted(sessionId),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-e2e", Map.of()),
                RunResponse.class);
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
        assertThat(types.stream().filter(this::isTerminal).count()).as("exactly one terminal event").isEqualTo(1);
        assertThat(types.get(types.size() - 1)).isIn(allowedTerminalTypes);
    }

    private void assertNoHangingRuntimeTasks() {
        assertThat(runtimeProbe.activeCount()).as("no hanging model/tool task remains").isZero();
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

    private String getText(String path, HttpHeaders requestHeaders) {
        ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.add("X-Pi-Dev-Tenant", "tenant-e2e");
        headers.add("X-Pi-Dev-User", "user-e2e");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static List<Long> sseIds(String body) {
        return body.lines()
                .filter(line -> line.startsWith("id:"))
                .map(line -> Long.parseLong(line.substring(3).trim()))
                .toList();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
