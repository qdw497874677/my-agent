package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Clock;
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

@SpringBootTest(classes = {PiCloudServerApplication.class, McpGovernedToolE2ETest.McpE2EConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class McpGovernedToolE2ETest {
    private static final String SECRET = "PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    McpE2EProbe probe;

    @Autowired
    InMemoryCloudE2EConfiguration.InMemoryStores stores;

    @Test
    void fakeMcpDiscoveryAndReadOnlyExecutionUseGatewayAuditEventsAndSchemaPassthrough() {
        RunOutcome outcome = runWithObjective("mcp read-only search");

        assertThat(outcome.status().status()).isEqualTo("COMPLETED");
        assertThat(probe.invocations()).isEqualTo(1);
        assertThat(outcome.events().events()).extracting(RunEventDto::type)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed", "run.completed");
        assertThat(outcome.events().events()).filteredOn(event -> "tool.completed".equals(event.type())).anySatisfy(event -> {
            assertThat(event.payloadSchema()).isEqualTo("tool.lifecycle");
            assertThat(event.payload().toString()).contains("mcp.fake.search", "sourceId=fake", "SUCCESS");
        });
        assertThat(stores.auditsForRun(outcome.run().runId())).extracting(InMemoryCloudE2EConfiguration.InMemoryStores.AuditRecord::action)
                .contains("tool.proposed", "tool.policy_decided", "tool.started", "tool.completed");
    }

    @Test
    void approvalAndBlockedMcpCallsDoNotInvokeRemoteTool() {
        int before = probe.invocations();

        RunOutcome approval = runWithObjective("mcp approval write");
        RunOutcome blocked = runWithObjective("mcp blocked destructive delete");

        assertThat(approval.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(approval.events().events()).extracting(RunEventDto::type)
                .contains("tool.preview_generated", "tool.approval_required")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(blocked.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(blocked.events().events()).extracting(RunEventDto::type)
                .contains("tool.denied")
                .doesNotContain("tool.started", "tool.completed");
        assertThat(probe.invocations()).isEqualTo(before);
    }

    @Test
    void authFailureServerDownAndTimeoutCancellationPathsStayRedactedAndGoverned() {
        RunOutcome auth = runWithObjective("mcp auth failure");
        RunOutcome timeout = runWithObjective("mcp timeout cancellation");
        post("/api/admin/governance/mcp/refresh", Map.of(), String.class);
        String governance = get("/api/admin/governance/mcp", String.class);

        assertThat(auth.status().status()).isEqualTo("POLICY_BLOCKED");
        assertThat(auth.events().toString()).contains("MCP_AUTH_FAILED").doesNotContain(SECRET);
        assertThat(timeout.status().status()).isIn("TIMED_OUT", "POLICY_BLOCKED");
        assertThat(timeout.events().toString()).contains("MCP_TIMEOUT").doesNotContain(SECRET);
        assertThat(governance).contains("server-down", "DISCOVERY_FAILED").doesNotContain(SECRET);
    }

    @Test
    void trustedStdioAndHttpTransportDescriptorsAreAvailableWithoutLaunchingProcessesOrNetwork() {
        String tools = get("/api/tools", String.class);

        assertThat(tools)
                .contains("mcp.stdio.echo", "mcp.http.echo", "STREAMABLE_HTTP", "STDIO")
                .contains("tool:mcp")
                .doesNotContain(SECRET);
    }

    private RunOutcome runWithObjective(String objective) {
        SessionResponse session = post("/api/sessions", new CreateSessionRequest("workspace-mcp-e2e", Map.of("source", "mcp-e2e")),
                SessionResponse.class);
        RunResponse run = post("/api/sessions/%s/runs".formatted(session.sessionId()),
                new CreateRunRequest("test-general-agent", "task", Map.of("objective", objective), "workspace-mcp-e2e", Map.of()),
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
        headers.add("X-Pi-Dev-Tenant", "tenant-mcp-e2e");
        headers.add("X-Pi-Dev-User", "user-mcp-e2e");
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

    static final class McpE2EProbe {
        private final AtomicInteger invocations = new AtomicInteger();

        int invocations() {
            return invocations.get();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class McpE2EConfiguration {
        @Bean
        McpE2EProbe mcpE2EProbe() {
            return new McpE2EProbe();
        }

        @Bean
        @Primary
        AgentRuntime agentRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    FakeModelClient model = new FakeModelClient();
                    model.script(new ModelResponse.ToolCallIntent(toolCall(context)));
                    model.script(new ModelResponse.FinalText("completed after MCP tool"));
                    return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink, new DeterministicIds(),
                            new DeterministicClock(Instant.parse("2026-06-16T09:00:00Z"))).start(context);
                }

                @Override
                public void cancel(String runId, String reason) {
                }
            };
        }

        private static ToolCall toolCall(RunContext context) {
            String objective = context.input().toString().toLowerCase();
            String runId = context.workspaceScope().runId();
            String toolId = objective.contains("approval") ? "mcp.fake.write_issue"
                    : objective.contains("blocked") ? "mcp.fake.delete_repo"
                    : objective.contains("auth") ? "mcp.fake.secret_echo"
                    : objective.contains("timeout") ? "mcp.fake.slow"
                    : "mcp.fake.search";
            return new ToolCall("mcp-call-" + runId, new RunId(runId), new StepId("mcp-step-" + runId), toolId,
                    Map.of("query", "pi", "secret", SECRET), Instant.parse("2026-06-16T09:00:00Z"));
        }

        @Bean
        @Primary
        ToolRegistry toolRegistry(McpE2EProbe probe) {
            List<ToolDescriptor> descriptors = List.of(
                    descriptor("mcp.fake.search", "fake", "search", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, "STREAMABLE_HTTP"),
                    descriptor("mcp.fake.write_issue", "fake", "write_issue", ToolRiskLevel.MEDIUM, ToolSideEffect.EXTERNAL_WRITE, "STREAMABLE_HTTP"),
                    descriptor("mcp.fake.delete_repo", "fake", "delete_repo", ToolRiskLevel.HIGH, ToolSideEffect.DESTRUCTIVE, "STREAMABLE_HTTP"),
                    descriptor("mcp.fake.secret_echo", "fake", "secret_echo", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, "STREAMABLE_HTTP"),
                    descriptor("mcp.fake.slow", "fake", "slow", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, "STREAMABLE_HTTP"),
                    descriptor("mcp.stdio.echo", "stdio", "echo", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, "STDIO"),
                    descriptor("mcp.http.echo", "http", "echo", ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, "STREAMABLE_HTTP"));
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

        private static ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken token, McpE2EProbe probe) {
            if (token.isCancellationRequested()) {
                return result(request, ToolExecutionStatus.CANCELLED, "MCP_INVOCATION_CANCELLED", Map.of("mcp.redactionHint", "cancelled"));
            }
            probe.invocations.incrementAndGet();
            if (request.toolId().endsWith("secret_echo")) {
                return result(request, ToolExecutionStatus.FAILED, "MCP_AUTH_FAILED", Map.of("mcp.actionHint", "Check MCP server credentials."));
            }
            if (request.toolId().endsWith("slow")) {
                return result(request, ToolExecutionStatus.TIMED_OUT, "MCP_TIMEOUT", Map.of("mcp.actionHint", "Check MCP server latency."));
            }
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "MCP fake tool completed", Optional.empty(), Map.of("mcp.serverId", "fake", "mcp.redactionHint", "summary-only"),
                    Map.of("answer", "redacted"), Set.of(), Optional.empty(), Duration.ZERO,
                    Optional.of(Map.of("mcp.structuredContent", Map.of("answer", 42))));
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
                    return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.DENY, "destructive MCP tool blocked", "mcp-e2e-policy",
                            false, Optional.empty(), Optional.empty(), Map.of());
                }
                if (request.descriptor().id().contains("write")) {
                    return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL, "MCP write requires approval", "mcp-e2e-policy",
                            true, Optional.of("approval:mcp"), Optional.empty(), Map.of());
                }
                return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.ALLOW, "MCP read-only allowed", "mcp-e2e-policy",
                        false, Optional.empty(), Optional.empty(), Map.of());
            };
        }

        @Bean
        @Primary
        McpServerRegistry fakeMcpServerRegistry(Clock clock) {
            McpServerProperties fake = McpServerProperties.streamableHttp("fake", "Fake MCP", "https://mcp.fake.test", "/mcp",
                    McpAuthProperties.bearerTokenRef("env:FAKE_MCP_TOKEN"), Map.of("secretRef", "env:PHASE7"));
            McpServerProperties down = McpServerProperties.streamableHttp("server-down", "Down MCP", "https://down.fake.test", "/mcp",
                    McpAuthProperties.none(), Map.of());
            McpServerProperties stdio = new McpServerProperties("stdio", true, "Trusted stdio MCP",
                    io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind.STDIO, null, null, "trusted-mcp", List.of("--stdio"),
                    Map.of(), Duration.ofSeconds(2), McpAuthProperties.none(), Map.of());
            return new McpServerRegistry(List.of(fake, down, stdio), server -> {
                if (server.id().equals("server-down")) {
                    throw new RuntimeException("connection failed token=" + SECRET);
                }
                return new McpServerRegistry.DiscoveryClient() {
                    @Override
                    public List<McpSchema.Tool> listTools() {
                        return List.of(new McpSchema.Tool("search", null, "Search", new McpSchema.JsonSchema("object",
                                Map.of("query", Map.of("type", "string")), List.of("query"), null, Map.of(), Map.of()), null,
                                new McpSchema.ToolAnnotations("Search", true, false, true, false, null), Map.of()));
                    }

                    @Override
                    public void close() {
                    }
                };
            }, clock);
        }

        @Bean
        @Primary
        io.github.pi_java.agent.domain.agent.AgentDefinition agentDefinition() {
            return new io.github.pi_java.agent.domain.agent.AgentDefinition(new AgentId("test-general-agent"), "Test General Agent",
                    "MCP governed E2E runtime", "fake-model", Set.of("tool:mcp"), Set.of("mcp-e2e-policy"),
                    new io.github.pi_java.agent.domain.agent.RuntimeLimits(Duration.ofSeconds(30), 8, 8),
                    Set.of(io.github.pi_java.agent.domain.agent.InteractionMode.CHAT, io.github.pi_java.agent.domain.agent.InteractionMode.TASK),
                    "test-workspace-policy", "test-output-policy");
        }

        private static ToolDescriptor descriptor(String id, String serverId, String toolName, ToolRiskLevel risk, ToolSideEffect sideEffect, String transport) {
            return new ToolDescriptor(id, toolName, "Fake MCP " + toolName,
                    new ToolSchema("json-schema", Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")),
                            "required", List.of("query")), Set.of("query"), 4096), Optional.empty(),
                    new ToolProvenance(ToolProvenance.SourceKind.MCP, serverId, "mcp:" + serverId + ":" + toolName, Map.of("transport", transport)),
                    "mcp", Set.of("tool:mcp", "mcp:server:" + serverId, "mcp:tool:" + serverId + ":" + toolName), risk, sideEffect,
                    Duration.ofSeconds(2), Map.of("mcp.serverId", serverId, "mcp.toolName", toolName, "mcp.transport", transport,
                    "mcp.remote", true, "mcp.external", true));
        }
    }
}
