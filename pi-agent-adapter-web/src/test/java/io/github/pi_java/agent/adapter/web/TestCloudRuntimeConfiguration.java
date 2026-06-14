package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolResult;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.FakeToolInvoker;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestCloudRuntimeConfiguration {

    @Bean
    RuntimeProbe runtimeProbe() {
        return new RuntimeProbe();
    }

    @Bean
    @Primary
    AgentRuntime agentRuntime(EventSink eventSink, RuntimeProbe runtimeProbe) {
        return new ScriptedCloudE2ERuntime(eventSink, runtimeProbe);
    }

    static final class RuntimeProbe {
        private final AtomicInteger runningStarts = new AtomicInteger();
        private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();

        void started(String runId) {
            runningStarts.incrementAndGet();
            activeRuns.add(runId);
        }

        void finished(String runId) {
            activeRuns.remove(runId);
            runningStarts.decrementAndGet();
        }

        int activeCount() {
            return activeRuns.size();
        }
    }

    private static final class ScriptedCloudE2ERuntime implements AgentRuntime {
        private final EventSink eventSink;
        private final RuntimeProbe runtimeProbe;
        private final DeterministicIds ids = new DeterministicIds();

        private ScriptedCloudE2ERuntime(EventSink eventSink, RuntimeProbe runtimeProbe) {
            this.eventSink = eventSink;
            this.runtimeProbe = runtimeProbe;
        }

        @Override
        public RunHandle start(RunContext context) {
            String runId = context.workspaceScope().runId();
            runtimeProbe.started(runId);
            try {
                if (inputText(context).contains("timeout")) {
                    sleep(Duration.ofSeconds(2));
                    return new RunHandle(runId, io.github.pi_java.agent.domain.runtime.RunStatus.FAILED, Optional.empty());
                }

                RuntimeLimits limits = inputText(context).contains("max-step")
                        ? new RuntimeLimits(Duration.ofSeconds(30), 1, 1)
                        : context.limits();
                FakeModelClient model = new FakeModelClient();
                FakeToolInvoker tools = new FakeToolInvoker();
                if (inputText(context).contains("max-step")) {
                    ToolCall call = new ToolCall("tool-call-" + runId, new RunId(runId), new StepId("planned-step-" + runId),
                            "fake.lookup", Map.of("query", "max-step"), Instant.parse("2026-06-14T00:00:00Z"));
                    model.script(new ModelResponse.ToolCallIntent(call));
                    tools.register("fake.lookup", new ToolResult(call.toolCallId(), true, "tool result", null, Instant.parse("2026-06-14T00:00:01Z")));
                } else {
                    model.script(new ModelResponse.FinalText("completed by fake runtime"));
                }
                RunContext scripted = new RunContext(agentDefinition(limits), context.input(), context.sessionContext(),
                        context.workspaceScope(), limits, context.cancellationToken(), context.traceId(), context.startedAt());
                return new GeneralAgentLoop(model, tools, FakePolicy.allow(), eventSink, ids,
                        new DeterministicClock(Instant.parse("2026-06-14T00:00:00Z"))).start(scripted);
            } finally {
                runtimeProbe.finished(runId);
            }
        }

        @Override
        public void cancel(String runId, String reason) {
            // Cancellation is observed through the shared CancellationToken in the dispatcher path.
        }

        private static AgentDefinition agentDefinition(RuntimeLimits limits) {
            return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "E2E fake runtime",
                    "fake-model", Set.of(), Set.of("fake"), limits, Set.of(InteractionMode.CHAT, InteractionMode.TASK),
                    "fake-workspace-policy", "fake-output-policy");
        }

        private static String inputText(RunContext context) {
            return context.input().toString().toLowerCase();
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
