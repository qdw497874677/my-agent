package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.extension.api.ModelProviderExtensionCapability;
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;
import io.github.pi_java.agent.infrastructure.extension.ExtensionModelProviderRegistry;
import io.github.pi_java.agent.testkit.DeterministicClock;
import io.github.pi_java.agent.testkit.DeterministicIds;
import io.github.pi_java.agent.testkit.FakeModelClient;
import io.github.pi_java.agent.testkit.FakePolicy;
import io.github.pi_java.agent.testkit.GeneralAgentLoop;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration(proxyBeanMethods = false)
class ExtensionConformanceFixtureConfiguration {

    static final String SAFE_TOOL = "extension.safe-read";
    static final String WORKSPACE_TOOL = "extension.workspace-write";
    static final String PROVIDER_ID = "phase6-extension-provider";
    static final String SECRET_MARKER = "PI_PHASE6_FAKE_SECRET_DO_NOT_LEAK";

    @Bean
    ExtensionConformanceProbe extensionConformanceProbe() {
        return new ExtensionConformanceProbe();
    }

    @Bean
    ExtensionSource extensionConformanceSource(ExtensionConformanceProbe probe) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata("phase6-product-extension", "Phase 6 Product Extension", "1.0.0", "Pi Test",
                        ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                        ExtensionLifecycleState.STARTED, ExtensionHealth.up("ready"), true,
                        Map.of("sourceKind", "SPRING_BEAN", "order", 10));
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(safeTool(probe), workspaceTool(probe), modelProvider());
            }
        };
    }

    @Bean
    @Primary
    AgentRuntime extensionConformanceRuntime(EventSink eventSink, ToolExecutionGateway toolExecutionGateway) {
        return new AgentRuntime() {
            @Override
            public RunHandle start(RunContext context) {
                FakeModelClient model = new FakeModelClient();
                model.script(new ModelResponse.ToolCallIntent(toolCall(context)));
                model.script(new ModelResponse.FinalText("completed after extension tool"));
                return new GeneralAgentLoop(model, toolExecutionGateway, FakePolicy.allow(), eventSink,
                        new DeterministicIds(), new DeterministicClock(Instant.parse("2026-06-16T00:00:00Z")))
                        .start(context);
            }

            @Override
            public void cancel(String runId, String reason) {
            }
        };
    }

    @Bean
    @Primary
    ToolPolicyEvaluator extensionConformancePolicy() {
        return request -> {
            if (request.descriptor().id().equals(WORKSPACE_TOOL)) {
                return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL,
                        "extension workspace writes require approval", "phase6-extension-policy", true,
                        Optional.of("approval:extension"), Optional.empty(), Map.of("extension", true));
            }
            return new ToolPolicyEvaluator.PolicyEvaluation(PolicyDecision.ALLOW, "extension safe read allowed",
                    "phase6-extension-policy", false, Optional.empty(), Optional.empty(), Map.of("extension", true));
        };
    }

    @Bean
    @Primary
    AgentDefinition extensionConformanceAgentDefinition() {
        return new AgentDefinition(new AgentId("test-general-agent"), "Test General Agent", "Extension conformance runtime",
                PROVIDER_ID + ":phase6-fake-model", Set.of("tool:read", "tool:workspace:write"),
                Set.of("phase6-extension-policy"), new RuntimeLimits(Duration.ofSeconds(30), 8, 8),
                Set.of(InteractionMode.CHAT, InteractionMode.TASK), "test-workspace-policy", "test-output-policy");
    }

    private static ToolCall toolCall(RunContext context) {
        String objective = context.input().toString().toLowerCase();
        String runId = context.workspaceScope().runId();
        String toolId = objective.contains("workspace") || objective.contains("approval") ? WORKSPACE_TOOL : SAFE_TOOL;
        Map<String, Object> arguments = toolId.equals(SAFE_TOOL)
                ? Map.of("prompt", "safe", "requestSecret", SECRET_MARKER)
                : Map.of("path", "extension/approval.txt", "content", "blocked", "requestSecret", SECRET_MARKER);
        return new ToolCall("extension-tool-call-" + runId,
                new io.github.pi_java.agent.domain.common.PlatformIds.RunId(runId),
                new io.github.pi_java.agent.domain.common.PlatformIds.StepId("extension-step-" + runId), toolId,
                arguments, Instant.parse("2026-06-16T00:00:00Z"));
    }

    private static ToolExtensionCapability safeTool(ExtensionConformanceProbe probe) {
        return new ToolExtensionCapability(SAFE_TOOL, descriptor(SAFE_TOOL, ToolSideEffect.READ_ONLY), (request, token) -> {
            probe.safeExecutions.incrementAndGet();
            Map<String, Object> raw = Map.of("message", "extension-safe", "secret", SECRET_MARKER, "large", "z".repeat(700));
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "extension safe read returned redacted data", Optional.empty(), Map.of(),
                    Map.of("message", "extension-safe", "secret", "[REDACTED]"), Set.of("secret"), Optional.empty(),
                    Duration.ZERO, Optional.of(raw));
        }, Map.of("sourceKind", "SPRING_BEAN", "boundary", "ToolExecutionGateway"));
    }

    private static ToolExtensionCapability workspaceTool(ExtensionConformanceProbe probe) {
        return new ToolExtensionCapability(WORKSPACE_TOOL, descriptor(WORKSPACE_TOOL, ToolSideEffect.WORKSPACE_WRITE),
                (request, token) -> {
                    probe.workspaceSideEffects.incrementAndGet();
                    return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                            "extension workspace write executed", Optional.empty(), Map.of(), Map.of("path", "extension/approval.txt"),
                            Set.of(), Optional.empty(), Duration.ZERO, Optional.of(Map.of("path", "extension/approval.txt")));
                }, Map.of("sourceKind", "SPRING_BEAN", "boundary", "WorkspaceGateway"));
    }

    private static ModelProviderExtensionCapability modelProvider() {
        return new ModelProviderExtensionCapability("extension.provider", PROVIDER_ID, Set.of("streaming", "tool-calls"),
                Map.of("sourceKind", "SPRING_BEAN", "defaultModel", "phase6-fake-model",
                        "displayName", "Phase 6 Extension Provider", "credentialRef", "env:PI_PHASE6_FAKE_SECRET"));
    }

    private static ToolDescriptor descriptor(String id, ToolSideEffect sideEffect) {
        return new ToolDescriptor(id, id, "Extension conformance tool",
                new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                        Map.of("type", "object", "additionalProperties", true), Set.of("requestSecret", "secret"), 16_384),
                Optional.empty(), new ToolProvenance(ToolProvenance.SourceKind.SPRING_BEAN,
                "phase6-product-extension", id, Map.of("extension.sourceKind", "SPRING_BEAN")),
                "1.0.0", Set.of("tool:read"), sideEffect == ToolSideEffect.READ_ONLY ? ToolRiskLevel.LOW : ToolRiskLevel.MEDIUM,
                sideEffect, Duration.ofSeconds(2), Map.of("extension.sourceKind", "SPRING_BEAN"));
    }

    static final class ExtensionConformanceProbe {
        private final AtomicInteger safeExecutions = new AtomicInteger();
        private final AtomicInteger workspaceSideEffects = new AtomicInteger();

        int safeExecutions() {
            return safeExecutions.get();
        }

        int workspaceSideEffects() {
            return workspaceSideEffects.get();
        }
    }
}
