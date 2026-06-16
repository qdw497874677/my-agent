package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
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
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {PiCloudServerApplication.class, PluginGovernedToolE2ETest.PluginE2EConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class PluginGovernedToolE2ETest {
    private static final String SECRET = "PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    PluginE2EProbe probe;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void pluginGovernedToolProductPathUsesGatewayPolicyAuditAndEvents() {
        RunOutcome outcome = runWithObjective("plugin read-only lookup");

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertThat(probe.invocations()).isEqualTo(1);
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(outcome.events().events()).filteredOn(event -> "tool.completed".equals(event.type())).anySatisfy(event -> {
            assertThat(event.payloadSchema()).isEqualTo("tool.lifecycle");
            assertThat(event.payload().toString())
                    .contains("plugin.fake.read", "sourceId=fake-plugin", "SUCCESS", "ToolExecutionGateway")
                    .doesNotContain(SECRET);
        });
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(InMemoryCloudE2EConfiguration.InMemoryStores.AuditRecord::action)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed");
        assertThat(stores.auditsForRun(outcome.run().runId()).toString())
                .contains("plugin.fake.read", "policyDecision=ALLOW", "tool.completed")
                .doesNotContain(SECRET);
    }

    @Test
    void pluginPolicyApprovalAndDenyBranchesDoNotInvokePluginBinding() {
        int before = probe.invocations();

        RunOutcome approval = runWithObjective("plugin approval write");
        RunOutcome denied = runWithObjective("plugin destructive delete");

        assertThat(approval.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(approval.events().events()).extracting(RunEventDto::type)
                .contains("tool.preview_generated", "tool.approval_required")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(denied.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(denied.events().events()).extracting(RunEventDto::type)
                .contains("tool.denied")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(probe.invocations()).isEqualTo(before);
    }

    @Test
    void pluginCatalogExposesPluginProvenanceWithoutRawPluginSecrets() {
        String tools = get("/api/tools", String.class);

        assertThat(tools)
                .contains("plugin.fake.read", "plugin.fake.write", "plugin.fake.delete", "PLUGIN", "fake-plugin", "capability:fake-plugin:read")
                .contains("tool:plugin")
                .doesNotContain(SECRET, "/var/secret/plugins", "secretValue");
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-plugin-e2e", Map.of("source", "plugin-e2e")),
                SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-plugin-e2e", Map.of()),
                RunResponse.class);
        RunStatusResponse status = awaitTerminal(session.sessionId(), run.runId());
        EventHistoryResponse events = get("/api/sessions/%s/runs/%s/events?afterSequence=0&limit=500".formatted(session.sessionId(), run.runId()),
                EventHistoryResponse.class);
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
        headers.add("X-Pi-Dev-Tenant", "tenant-plugin-e2e");
        headers.add("X-Pi-Dev-User", "user-plugin-e2e");
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

    static final class PluginE2EProbe {
        private final AtomicInteger invocations = new AtomicInteger();

        int invocations() {
            return invocations.get();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PluginE2EConfiguration {
        @Bean
        PluginE2EProbe pluginE2EProbe() {
            return new PluginE2EProbe();
        }

        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    model.script(new ModelResponse.ToolCallIntent(toolCall(context)));
                    model.script(new ModelResponse.FinalText("completed after plugin tool"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-16T10:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        private static ToolCall toolCall(RunContext context) {
            String objective = context.input().toString().toLowerCase();
            String runId = context.workspaceScope().runId();
            String toolId = objective.contains("approval") ? "plugin.fake.write"
                    : objective.contains("destructive") ? "plugin.fake.delete"
                    : "plugin.fake.read";
            return new ToolCall("plugin-call-" + runId, new RunId(runId), new StepId("plugin-step-" + runId), toolId,
                    Map.of("query", "pi", "secret", SECRET), Instant.parse("2026-06-16T10:00:00Z"));
        }

        @Bean
        @Primary
        PluginGovernanceCatalog pluginGovernanceCatalog() {
            return new PluginGovernanceCatalog() {
                @Override
                public java.util.List<io.github.pi_java.agent.app.port.plugin.PluginSourceStatus> plugins() {
                    return java.util.List.of();
                }

                @Override
                public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus refresh() {
                    return new io.github.pi_java.agent.app.port.plugin.PluginMutationStatus(true, "", "refresh", "", "", "REFRESH_REQUESTED", "", Map.of());
                }

                @Override
                public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus disable(String pluginId, String actor, String reason) {
                    return mutation(pluginId, "disable", "DISABLED");
                }

                @Override
                public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
                    return mutation(pluginId, "quarantine", "QUARANTINED");
                }

                private io.github.pi_java.agent.app.port.plugin.PluginMutationStatus mutation(String pluginId, String operation, String status) {
                    return new io.github.pi_java.agent.app.port.plugin.PluginMutationStatus(true, pluginId, operation, "STARTED", status, status, "", Map.of());
                }
            };
        }

        @Bean
        @Primary
        ToolRegistry toolRegistry(PluginE2EProbe probe) {
            List<ToolDescriptor> descriptors = List.of(
                    descriptor("plugin.fake.read", "read", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY),
                    descriptor("plugin.fake.write", "write", ToolRiskLevel.MEDIUM, ToolSideEffect.EXTERNAL_WRITE),
                    descriptor("plugin.fake.delete", "delete", ToolRiskLevel.HIGH, ToolSideEffect.DESTRUCTIVE));
            return new ToolRegistry() {
                @Override
                public List<ToolDescriptor> listTools() {
                    return descriptors;
                }

                @Override
                public Optional<ToolResolution> resolve(String toolId) {
                    return descriptors.stream().filter(descriptor -> descriptor.id().equals(toolId)).findFirst()
                            .map(descriptor -> new ToolResolution(descriptor, (request, token) -> execute(request, token, probe)));
                }
            };
        }

        private static ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken token, PluginE2EProbe probe) {
            if (token.isCancellationRequested()) {
                return result(request, ToolExecutionStatus.CANCELLED, "PLUGIN_CANCELLED", Map.of("gateway", "ToolExecutionGateway"));
            }
            probe.invocations.incrementAndGet();
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "Plugin fake tool completed through ToolExecutionGateway", Optional.empty(),
                    Map.of("gateway", "ToolExecutionGateway", "plugin.id", "fake-plugin"), Map.of("ok", true), Set.of(),
                    Optional.empty(), Duration.ZERO, Optional.of(Map.of("ok", true)));
        }

        private static ToolExecutionResult result(ToolExecutionRequest request, ToolExecutionStatus status, String category, Map<String, Object> summary) {
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), status, category,
                    Optional.of(category), summary, Map.of(), Set.of(), Optional.empty(), Duration.ZERO, Optional.empty());
        }

        @Bean
        @Primary
        ToolPolicyEvaluator toolPolicyEvaluator() {
            return request -> {
                if (request.descriptor().id().contains("delete")) {
                    return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.DENY, "destructive plugin tool blocked", "plugin-e2e-policy",
                            false, Optional.empty(), Optional.empty(), Map.of("sourceKind", "PLUGIN"));
                }
                if (request.descriptor().id().contains("write")) {
                    return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL, "plugin write requires approval", "plugin-e2e-policy",
                            true, Optional.of("approval:plugin"), Optional.empty(), Map.of("sourceKind", "PLUGIN"));
                }
                return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.ALLOW, "plugin read-only allowed", "plugin-e2e-policy",
                        false, Optional.empty(), Optional.empty(), Map.of("sourceKind", "PLUGIN"));
            };
        }

        @Bean
        @Primary
        AgentDefinition agentDefinition() {
            return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "Plugin governed E2E runtime",
                    "fake-model", Set.of("tool:plugin"), Set.of("plugin-e2e-policy"), new RuntimeLimits(Duration.ofSeconds(30), 8, 8),
                    Set.of(InteractionMode.CHAT, InteractionMode.TASK), "test-workspace-policy", "test-output-policy");
        }

        private static ToolDescriptor descriptor(String id, String capability, ToolRiskLevel risk, ToolSideEffect sideEffect) {
            return new ToolDescriptor(id, "Fake plugin " + capability, "Fake read-only plugin tool capability " + capability,
                    new ToolSchema("json-schema", Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")),
                            "required", List.of("query")), Set.of("query"), 4096), Optional.empty(),
                    new ToolProvenance(ToolProvenance.SourceKind.PLUGIN, "fake-plugin", "capability:fake-plugin:" + capability,
                            Map.of("plugin.id", "fake-plugin", "plugin.sourceId", "fake-plugin", "plugin.capabilityId", id,
                                    "plugin.sourcePath", "fake-plugin.jar", "gateway", "ToolExecutionGateway")),
                    "1.0.0", Set.of("tool:plugin", "plugin:fake-plugin", "plugin:capability:" + capability), risk, sideEffect,
                    Duration.ofSeconds(2), Map.of("plugin.id", "fake-plugin", "plugin.capabilityId", id,
                    "gateway", "ToolExecutionGateway"));
        }
    }
}
