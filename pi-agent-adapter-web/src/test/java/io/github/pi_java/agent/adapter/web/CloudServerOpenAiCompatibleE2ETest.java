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

@SpringBootTest(classes = {PiCloudServerApplication.class, FakeOpenAiProviderE2EConfiguration.class, InMemoryCloudE2EConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pi.providers.openai-compatible.enabled=true",
        "pi.providers.openai-compatible.api-key=sk-fake-e2e-secret",
        "pi.providers.openai-compatible.default-model-id=gpt-fake-e2e",
        "pi.runtime.default-model-ref=openai-compatible:gpt-fake-e2e",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class CloudServerOpenAiCompatibleE2ETest {

    private static final String FAKE_SECRET = "sk-fake-e2e-secret";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void fakeOpenAiCompatibleProviderStreamsPersistsAndReplaysModelDeltaEventsWithoutSecrets() {
        SessionResponse session = createSession("workspace-openai-e2e");
        RunResponse run = createRun(session.sessionId(), "stream with fake OpenAI-compatible provider");

        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        RunResultResponse result = get("/api/sessions/%s/runs/%s/result".formatted(session.sessionId(), run.runId()), RunResultResponse.class);
        assertThat(status.status()).as("run result: %s", result).isEqualTo("COMPLETED");

        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500"
                .formatted(session.sessionId(), run.runId()), EventHistoryResponse.class);
        assertThat(events.events()).extracting(RunEventDto::type).contains("model.delta", "run.completed");
        assertThat(events.events().stream().filter(event -> event.type().equals("model.delta")).toList()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.toString()).doesNotContain(FAKE_SECRET);
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    private SessionResponse createSession(String workspaceId) {
        return post("/api/sessions", new CreateSessionRequest(workspaceId, Map.of("source", "openai-e2e")), SessionResponse.class);
    }

    private RunResponse createRun(String sessionId, String objective) {
        return post("/api/sessions/%s/runs".formatted(sessionId),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-openai-e2e", Map.of()),
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
        headers.add("X-Pi-Dev-Tenant", "tenant-openai-e2e");
        headers.add("X-Pi-Dev-User", "user-openai-e2e");
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
