package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolResult;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakeStreamingModelClient;
import io.github.pi_java.agent.testkit.FakeToolInvoker;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Path;

/** Test-classpath fixture used by Playwright to run the Web Console without model keys, Docker, or external services. */
@Configuration(proxyBeanMethods = false)
@Profile("e2e")
@Import(InMemoryCloudE2EConfiguration.class)
public class WebConsoleE2EFixtureConfiguration {

    @Bean
    @Primary
    AgentRuntime webConsoleE2ERuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
        return new ScriptedWebConsoleRuntime(eventSink, toolExecutionGateway);
    }

    @Bean
    @Primary
    McpServerRegistry webConsoleE2EMcpServerRegistry(Clock clock) {
        McpServerProperties healthy = McpServerProperties.streamableHttp("e2e", "E2E Fake MCP", "https://mcp.e2e.test", "/mcp",
                McpAuthProperties.bearerTokenRef("env:PI_E2E_FAKE_MCP_TOKEN"), Map.of("fixture", "phase-07"));
        McpServerProperties unhealthy = McpServerProperties.streamableHttp("server-down", "Down Fake MCP", "https://down.e2e.test", "/mcp",
                McpAuthProperties.none(), Map.of());
        McpServerRegistry registry = new McpServerRegistry(List.of(healthy, unhealthy), server -> {
            if ("server-down".equals(server.id())) {
                throw new RuntimeException("connection failed token=PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK");
            }
            return new McpServerRegistry.DiscoveryClient() {
                @Override
                public List<McpSchema.Tool> listTools() {
                    return List.of(new McpSchema.Tool("echo", null, "E2E echo",
                            new McpSchema.JsonSchema("object", Map.of("message", Map.of("type", "string")), List.of("message"), null, Map.of(), Map.of()),
                            null, new McpSchema.ToolAnnotations("Echo", true, false, true, false, null), Map.of()));
                }

                @Override
                public void close() {
                }
            };
        }, clock);
        registry.refresh();
        return registry;
    }

    @Bean
    @Primary
    PluginGovernanceCatalog webConsoleE2EPluginGovernanceCatalog() {
        return new PluginGovernanceCatalog() {
            @Override
            public List<PluginSourceStatus> plugins() {
                return List.of(
                        new PluginSourceStatus("healthy-plugin", "Healthy Plugin", "1.0.0", "Pi Test", "PF4J_JAR",
                                "STARTED", true, "UP", "COMPATIBLE", 1, Map.of("USABLE", "1"), "",
                                "plugins/healthy-plugin.jar", "", Instant.parse("2026-06-16T13:00:00Z"),
                                List.of(new PluginCapabilityStatus("plugin.healthy.read", "TOOL", "USABLE", "1.0.0",
                                        "healthy-plugin", true, "COMPATIBLE", "UP", Map.of("risk", "low"))),
                                Map.of("fixture", "phase-08")),
                        new PluginSourceStatus("disabled-plugin", "Disabled Plugin", "1.0.0", "Pi Test", "PF4J_JAR",
                                "DISABLED", false, "UP", "COMPATIBLE", 0, Map.of(), "",
                                "plugins/disabled-plugin.jar", "operator disabled", Instant.parse("2026-06-16T13:01:00Z"),
                                List.of(), Map.of("fixture", "phase-08")),
                        new PluginSourceStatus("quarantined-plugin", "Quarantined Plugin", "1.0.0", "Pi Test", "PF4J_JAR",
                                "QUARANTINED", false, "WARN", "COMPATIBLE", 0, Map.of(), "",
                                "plugins/quarantined-plugin.jar", "operator quarantine", Instant.parse("2026-06-16T13:02:00Z"),
                                List.of(), Map.of("fixture", "phase-08")),
                        new PluginSourceStatus("failed-plugin", "Failed Plugin", "9.9.9", "Pi Test", "PF4J_JAR",
                                "FAILED", false, "DOWN", "INCOMPATIBLE", 0, Map.of(),
                                "[REDACTED] load failure", "plugins/failed-plugin.jar", "",
                                Instant.parse("2026-06-16T13:03:00Z"), List.of(), Map.of("operatorHint", "check compatibility"))
                );
            }

            @Override
            public PluginMutationStatus refresh() {
                return new PluginMutationStatus(true, "", "refresh", "", "", "REFRESH_REQUESTED", "",
                        Map.of("fixture", "phase-08"));
            }

            @Override
            public PluginMutationStatus disable(String pluginId, String actor, String reason) {
                return mutation(pluginId, "disable", "DISABLED");
            }

            @Override
            public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
                return mutation(pluginId, "quarantine", "QUARANTINED");
            }

            private PluginMutationStatus mutation(String pluginId, String operation, String resulting) {
                return new PluginMutationStatus(true, pluginId, operation, "STARTED", resulting, resulting, "",
                        Map.of("actor", "e2e-user", "reason", "[REDACTED]"));
            }
        };
    }

    @RestController
    static final class PlaywrightReadyController {
        private final Path readyFile;

        PlaywrightReadyController(@Value("${pi.e2e.ready-file:/tmp/pi-java-playwright-ready}") String readyFile) {
            this.readyFile = Path.of(readyFile);
        }

        @GetMapping("/__playwright_ready")
        ResponseEntity<String> ready() {
            return Files.exists(readyFile) ? ResponseEntity.ok("ready") : ResponseEntity.status(503).body("starting");
        }
    }

    private static final class ScriptedWebConsoleRuntime implements AgentRuntime {
        private final EventSink eventSink;
        private final ToolExecutionGateway toolExecutionGateway;
        private final DeterministicIds ids = new DeterministicIds();

        private ScriptedWebConsoleRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
            this.eventSink = eventSink;
            this.toolExecutionGateway = toolExecutionGateway;
        }

        @Override
        public RunHandle start(RunContext context) {
            String input = context.input().toString().toLowerCase();
            if (input.contains("cancel me")) {
                sleep(Duration.ofSeconds(3));
            }
            FakeToolInvoker tools = new FakeToolInvoker();
            String runId = context.workspaceScope().runId();
            if (input.contains("approval")) {
                FakeModelClient model = new FakeModelClient();
                ToolCall call = new ToolCall("approval-call-" + runId, new RunId(runId), new StepId("approval-step-" + runId),
                        "builtin.workspace.write", Map.of("path", "reports/e2e.txt", "content", "requires approval"), Instant.parse("2026-06-15T00:00:00Z"));
                model.script(new io.github.pi_java.agent.domain.model.ModelResponse.ToolCallIntent(call));
                tools.register("builtin.workspace.write", new ToolResult(call.toolCallId(), true, "approval should prevent execution", null, Instant.parse("2026-06-15T00:00:01Z")));
                return new GeneralAgentLoop(model, toolExecutionGateway, io.github.pi_java.agent.testkit.FakePolicy.allow(), eventSink, ids,
                        new DeterministicClock(Instant.parse("2026-06-15T00:00:00Z"))).start(context);
            } else if (input.contains("tool")) {
                FakeStreamingModelClient model = new FakeStreamingModelClient();
                ToolCall call = new ToolCall("tool-call-" + runId, new RunId(runId), new StepId("tool-step-" + runId),
                        "builtin.info", Map.of(), Instant.parse("2026-06-15T00:00:00Z"));
                model.text("Streaming fake answer. ");
                model.toolCall(call);
                model.text("Terminal fake result with governed tool summary.");
                model.finish(ModelFinishReason.STOP);
                tools.register("builtin.info", new ToolResult(call.toolCallId(), true, "redacted fake tool result", null, Instant.parse("2026-06-15T00:00:01Z")));
                return new GeneralAgentLoop(model, toolExecutionGateway, io.github.pi_java.agent.testkit.FakePolicy.allow(), eventSink, ids,
                        new DeterministicClock(Instant.parse("2026-06-15T00:00:00Z"))).start(context);
            } else {
                FakeStreamingModelClient model = new FakeStreamingModelClient();
                model.text("Streaming fake answer. ");
                model.text("Terminal fake result.");
                model.finish(ModelFinishReason.STOP);
                return new GeneralAgentLoop(model, toolExecutionGateway, io.github.pi_java.agent.testkit.FakePolicy.allow(), eventSink, ids,
                        new DeterministicClock(Instant.parse("2026-06-15T00:00:00Z"))).start(context);
            }
        }

        @Override
        public void cancel(String runId, String reason) {
            // The dispatcher observes cancellation through its CancellationToken and terminal publisher.
        }

        private static void sleep(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
