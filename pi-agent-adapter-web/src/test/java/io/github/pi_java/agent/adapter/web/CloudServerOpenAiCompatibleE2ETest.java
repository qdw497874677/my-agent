package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
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
import org.springframework.core.ParameterizedTypeReference;
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
        "pi.local.db-path=target/openai-compatible-e2e-local.db",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "com.vaadin.flow.spring.SpringBootAutoConfiguration,"
                + "com.vaadin.flow.spring.SpringSecurityAutoConfiguration,"
                + "com.vaadin.flow.spring.VaadinScopesConfig"
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
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500"
                .formatted(session.sessionId(), run.runId()), EventHistoryResponse.class);
        assertThat(status.status()).as("run result: %s, events: %s", result, events).isEqualTo("COMPLETED");
        assertThat(events.events()).extracting(RunEventDto::type).contains("model.delta", "run.completed");
        assertThat(events.events().stream().filter(event -> event.type().equals("model.delta")).toList()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.toString()).doesNotContain(FAKE_SECRET);
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void sameSessionConversationKeepsProviderMetadataAndRestoresTwoTurnTranscript() {
        SessionResponse session = createSession("workspace-openai-e2e-history");

        RunResponse firstRun = createRun(session.sessionId(), "first question for fake OpenAI-compatible provider");
        awaitCompleted(session.sessionId(), firstRun.runId());
        RunResponse secondRun = createRun(session.sessionId(), "follow up using the same session history");
        awaitCompleted(session.sessionId(), secondRun.runId());

        assertProviderSnapshot(firstRun);
        assertProviderSnapshot(secondRun);

        ConversationTranscriptResponse transcript = get(
                "/api/sessions/%s/transcript?limit=20".formatted(session.sessionId()),
                ConversationTranscriptResponse.class);
        assertThat(transcript.sessionId()).isEqualTo(session.sessionId());
        assertThat(transcript.messages()).extracting(ConversationMessageDto::role)
                .containsSubsequence(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT,
                        ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);
        assertThat(transcript.messages()).extracting(ConversationMessageDto::runId)
                .contains(firstRun.runId(), secondRun.runId());
        assertThat(transcript.messages()).extracting(ConversationMessageDto::text)
                .contains("first question for fake OpenAI-compatible provider",
                        "follow up using the same session history")
                .anySatisfy(text -> assertThat(text).contains("fake-openai delta"));
        assertThat(transcript.toString()).doesNotContain(FAKE_SECRET);

        PageResponse<SessionSummaryDto> recent = get("/api/sessions/recent?limit=20",
                new ParameterizedTypeReference<>() {
                });
        assertThat(recent.items()).anySatisfy(summary -> {
            assertThat(summary.sessionId()).isEqualTo(session.sessionId());
            assertThat(summary.title()).contains("first question");
            assertThat(summary.lastMessagePreview()).contains("fake-openai delta");
        });
        assertThat(recent.toString()).doesNotContain(FAKE_SECRET);
    }

    private SessionResponse createSession(String workspaceId) {
        return post("/api/sessions", new CreateSessionRequest(workspaceId, Map.of("source", "openai-e2e")), SessionResponse.class);
    }

    private RunResponse createRun(String sessionId, String objective) {
        return post("/api/sessions/%s/runs".formatted(sessionId),
                new CreateRunRequest("test-general-agent", "chat", Map.of("text", objective), "workspace-openai-e2e",
                        Map.of(
                                "selectedModelRef", "openai-compatible:gpt-fake-e2e",
                                "providerId", "openai-compatible",
                                "modelId", "gpt-fake-e2e",
                                "readinessState", "READY")),
                RunResponse.class);
    }

    private void awaitCompleted(String sessionId, String runId) {
        RunStatusResponse status = awaitTerminal(sessionId, runId);
        RunResultResponse result = get("/api/sessions/%s/runs/%s/result".formatted(sessionId, runId), RunResultResponse.class);
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500"
                .formatted(sessionId, runId), EventHistoryResponse.class);
        assertThat(status.status()).as("run result: %s, events: %s", result, events).isEqualTo("COMPLETED");
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    private static void assertProviderSnapshot(RunResponse run) {
        assertThat(run.providerMetadata().selectedModelRef()).isEqualTo("openai-compatible:gpt-fake-e2e");
        assertThat(run.providerMetadata().resolvedProviderId()).isEqualTo("openai-compatible");
        assertThat(run.providerMetadata().resolvedModelId()).isEqualTo("gpt-fake-e2e");
        assertThat(run.providerMetadata().readinessState()).isEqualTo("READY");
        assertThat(run.providerMetadata().toString()).doesNotContain(FAKE_SECRET);
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

    private <T> T get(String path, ParameterizedTypeReference<T> responseType) {
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
