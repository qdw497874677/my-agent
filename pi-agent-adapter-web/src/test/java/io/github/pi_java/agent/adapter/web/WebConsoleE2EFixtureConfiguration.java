package io.github.pi_java.agent.adapter.web;

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
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakeStreamingModelClient;
import io.github.pi_java.agent.testkit.FakeToolInvoker;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

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
                        "builtin.info", Map.of("topic", "phase-05"), Instant.parse("2026-06-15T00:00:00Z"));
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
