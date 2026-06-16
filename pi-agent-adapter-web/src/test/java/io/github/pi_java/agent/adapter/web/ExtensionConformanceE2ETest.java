package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PiCloudServerApplication.class, ExtensionConformanceFixtureConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "pi.providers.openai-compatible.enabled=false",
        "pi.providers.openai-compatible.api-key=PI_PHASE6_FAKE_SECRET_DO_NOT_LEAK"
})
class ExtensionConformanceE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ExtensionConformanceFixtureConfiguration.ExtensionConformanceProbe probe;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void restCreatedRunInvokesExtensionToolThroughGatewayEventsAuditAndRedaction() {
        RunOutcome outcome = runWithObjective("safe extension tool");

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertThat(probe.safeExecutions()).isEqualTo(1);
        assertThat(outcome.events().events()).extracting(event -> event.type())
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(outcome.events().toString()).contains("extension.safe-read", "redacted")
                .doesNotContain(ExtensionConformanceFixtureConfiguration.SECRET_MARKER);
        assertThat(stores.listByRun(outcome.run().runId(), 0, 500).toString())
                .contains("extension.safe-read")
                .doesNotContain(ExtensionConformanceFixtureConfiguration.SECRET_MARKER);
        assertThat(stores.auditsForRun(outcome.run().runId()).toString())
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed")
                .doesNotContain(ExtensionConformanceFixtureConfiguration.SECRET_MARKER);
    }

    @Test
    void approvalGatePreventsExtensionWorkspaceSideEffectAndRecordsLifecycle() {
        int before = probe.workspaceSideEffects();
        RunOutcome outcome = runWithObjective("approval workspace extension tool");

        assertThat(outcome.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(probe.workspaceSideEffects()).isEqualTo(before);
        assertThat(outcome.events().events()).extracting(event -> event.type())
                .contains("tool.proposed", "tool.policy_decided", "tool.preview_generated", "tool.approval_required", "run.policy_blocked")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(record -> record.action())
                .contains("tool.preview_generated", "tool.approval_required");
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-extension-conformance", Map.of()),
                SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective),
                        "workspace-extension-conformance", Map.of()), RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500"
                .formatted(session.sessionId(), run.runId()), EventHistoryResponse.class);
        return new RunOutcome(session, run, status, events);
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
        headers.add("X-Pi-Dev-Tenant", "tenant-extension-conformance");
        headers.add("X-Pi-Dev-User", "user-extension-conformance");
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

    private record RunOutcome(SessionResponse session, RunResponse run, RunStatusResponse status, EventHistoryResponse events) {
    }
}
