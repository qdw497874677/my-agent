package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {PiCloudServerApplication.class, SamplePluginJarE2ETest.SamplePluginRuntimeConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "pi.plugins.enabled=true",
        "pi.plugins.non-sandbox-warning-acknowledged=true",
        "pi.plugins.platform-api-version=1.0.0"
})
class SamplePluginJarE2ETest {
    static final String SAMPLE_PLUGIN_ID = "sample-readonly-plugin";
    static final String SAMPLE_TOOL_ID = "plugin.sample.readonly.lookup";
    private static final Path CONTROLLED_PLUGIN_DIR = prepareControlledPluginDirectory();

    @DynamicPropertySource
    static void pluginDirectory(DynamicPropertyRegistry registry) {
        registry.add("pi.plugins.directory", () -> CONTROLLED_PLUGIN_DIR.toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void cloudServerLoadsSamplePluginJarAndExposesReadOnlyCapability() {
        String plugins = get("/api/admin/governance/plugins", String.class);
        String tools = get("/api/tools", String.class);

        assertThat(plugins).contains(SAMPLE_PLUGIN_ID, "Sample read-only Pi extension plugin", SAMPLE_TOOL_ID,
                "sample-readonly-plugin");
        assertThat(tools).contains(SAMPLE_TOOL_ID, "PLUGIN", SAMPLE_PLUGIN_ID, "tool:plugin", "tool:read");
    }

    @Test
    void samplePluginToolRunsThroughGatewayAuditAndEvents() {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-sample-plugin", Map.of()),
                SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", "sample plugin lookup"),
                        "workspace-sample-plugin", Map.of()), RunResponse.class);

        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(
                session.sessionId(), run.runId()), EventHistoryResponse.class);

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(events.events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(events.events()).filteredOn(event -> "tool.completed".equals(event.type())).singleElement().satisfies(event ->
                assertThat(event.payload().toString()).contains(SAMPLE_TOOL_ID, SAMPLE_PLUGIN_ID,
                        "sample plugin read-only result"));
        assertThat(stores.auditsForRun(run.runId()).toString()).contains(SAMPLE_TOOL_ID, "tool.completed", "policyDecision=ALLOW");
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
        headers.add("X-Pi-Dev-Tenant", "tenant-sample-plugin");
        headers.add("X-Pi-Dev-User", "user-sample-plugin");
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

    static Path prepareControlledPluginDirectory() {
        try {
            Path directory = Files.createTempDirectory("pi-sample-plugin-controlled-");
            Path sampleJar = samplePluginJar();
            Files.copy(sampleJar, directory.resolve(sampleJar.getFileName()));
            directory.toFile().deleteOnExit();
            return directory;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare controlled sample plugin directory", ex);
        }
    }

    static Path samplePluginJar() throws IOException {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (root.getFileName().toString().equals("pi-agent-adapter-web")) {
            root = root.getParent();
        }
        Path target = root.resolve("pi-sample-plugin-readonly/target");
        try (var stream = Files.list(target)) {
            return stream.filter(path -> path.getFileName().toString().matches("pi-sample-plugin-readonly-.*\\.jar"))
                    .filter(path -> !path.getFileName().toString().contains("sources"))
                    .max(Comparator.comparing(path -> path.toFile().lastModified()))
                    .orElseThrow(() -> new IllegalStateException("Sample plugin jar not found in " + target
                            + "; run `mvn -pl pi-sample-plugin-readonly -am package` first."));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SamplePluginRuntimeConfiguration {
        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    String runId = context.workspaceScope().runId();
                    model.script(new ModelResponse.ToolCallIntent(new ToolCall("sample-plugin-call-" + runId, new RunId(runId),
                            new StepId("sample-plugin-step-" + runId), SAMPLE_TOOL_ID, Map.of("query", "pi"),
                            Instant.parse("2026-06-16T10:00:00Z"))));
                    model.script(new ModelResponse.FinalText("completed after sample plugin tool"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-16T10:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        @Bean
        @Primary
        ToolPolicyEvaluator toolPolicyEvaluator() {
            return request -> new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.ALLOW, "sample plugin read-only allowed",
                    "sample-plugin-policy", false, java.util.Optional.empty(), java.util.Optional.empty(),
                    Map.of("sourceKind", "PLUGIN"));
        }

        @Bean
        @Primary
        AgentDefinition agentDefinition() {
            return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "Sample plugin E2E runtime",
                    "fake-model", Set.of("tool:plugin", "tool:read"), Set.of("sample-plugin-policy"),
                    new RuntimeLimits(Duration.ofSeconds(30), 8, 8), Set.of(InteractionMode.CHAT, InteractionMode.TASK),
                    "test-workspace-policy", "test-output-policy");
        }
    }
}
