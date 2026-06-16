package io.github.pi_java.agent.extension.api.fixtures;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.extension.api.EventListenerExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.extension.api.MemoryProviderExtensionCapability;
import io.github.pi_java.agent.extension.api.ModelProviderExtensionCapability;
import io.github.pi_java.agent.extension.api.PolicyExtensionCapability;
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;
import io.github.pi_java.agent.extension.api.WorkspaceProviderExtensionCapability;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * No-key conformance fixtures for extension source parity tests.
 *
 * <p>This source set is intentionally lightweight: modules may duplicate or copy these values instead of wiring a
 * Maven test-fixtures dependency, but the IDs and metadata here are the canonical Phase 6 conformance contract.</p>
 */
public final class ConformanceExtensionSources {

    public static final String SOURCE_ID = "phase6-conformance-extension";
    public static final String TOOL_SAFE_READ = "phase6.extension.safe-read";
    public static final String TOOL_WORKSPACE_WRITE = "phase6.extension.workspace-write";
    public static final String EVENT_LISTENER = "phase6.extension.audit-listener";
    public static final String MODEL_PROVIDER = "phase6-extension-provider";
    public static final String POLICY = "phase6-extension-policy";
    public static final String WORKSPACE_PROVIDER = "phase6-extension-workspace";
    public static final String MEMORY_PROVIDER = "phase6-extension-memory";
    public static final String SECRET_MARKER = "PI_PHASE6_FAKE_SECRET_DO_NOT_LEAK";
    public static final String SECRET_REF = "env:PI_PHASE6_FAKE_SECRET";

    private ConformanceExtensionSources() {
    }

    public static ExtensionSource spiSource() {
        return source(ToolProvenance.SourceKind.SPI, 10);
    }

    public static ExtensionSource springBeanSource() {
        return source(ToolProvenance.SourceKind.SPRING_BEAN, 10);
    }

    public static ExtensionSource source(ToolProvenance.SourceKind sourceKind, int order) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata(SOURCE_ID, "Phase 6 Conformance Extension", "1.0.0", "Pi Test",
                        ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                        ExtensionLifecycleState.STARTED, ExtensionHealth.up("conformance-ready"), true,
                        Map.of("sourceKind", sourceKind.name(), "order", order));
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(safeReadTool(sourceKind), workspaceWriteTool(sourceKind), eventListener(sourceKind),
                        modelProvider(sourceKind), policyProvider(sourceKind), workspaceProvider(sourceKind),
                        memoryProvider(sourceKind));
            }
        };
    }

    public static ToolExtensionCapability safeReadTool(ToolProvenance.SourceKind sourceKind) {
        return new ToolExtensionCapability(TOOL_SAFE_READ, descriptor(TOOL_SAFE_READ, sourceKind, ToolSideEffect.READ_ONLY),
                (request, cancellationToken) -> new ToolExecutionResult(request.toolCallId(), request.toolId(),
                        ToolExecutionStatus.SUCCESS, "extension safe read completed", Optional.empty(), Map.of(),
                        Map.of("answer", "safe", "secret", "[REDACTED]"), Set.of("secret"), Optional.empty(),
                        Duration.ZERO, Optional.of(Map.of("answer", "safe", "secret", SECRET_MARKER))),
                Map.of("sourceKind", sourceKind.name(), "order", 10, "version", "1.0.0"));
    }

    public static ToolExtensionCapability workspaceWriteTool(ToolProvenance.SourceKind sourceKind) {
        return new ToolExtensionCapability(TOOL_WORKSPACE_WRITE,
                descriptor(TOOL_WORKSPACE_WRITE, sourceKind, ToolSideEffect.WORKSPACE_WRITE),
                (request, cancellationToken) -> new ToolExecutionResult(request.toolCallId(), request.toolId(),
                        ToolExecutionStatus.SUCCESS, "extension workspace write completed", Optional.empty(), Map.of(),
                        Map.of("workspace", "bounded"), Set.of(), Optional.empty(), Duration.ZERO,
                        Optional.of(Map.of("workspace", "bounded"))),
                Map.of("sourceKind", sourceKind.name(), "order", 20, "workspaceBoundary", "WorkspaceGateway"));
    }

    public static EventListenerExtensionCapability eventListener(ToolProvenance.SourceKind sourceKind) {
        return new EventListenerExtensionCapability(EVENT_LISTENER, Set.of("tool.lifecycle"),
                Map.of("sourceKind", sourceKind.name(), "order", 30, "version", "1.0.0"));
    }

    public static ModelProviderExtensionCapability modelProvider(ToolProvenance.SourceKind sourceKind) {
        return new ModelProviderExtensionCapability("phase6.extension.provider", MODEL_PROVIDER,
                Set.of("streaming", "tool-calls"), Map.of("sourceKind", sourceKind.name(), "order", 40,
                "defaultModel", "phase6-fake-model", "displayName", "Phase 6 Fake Provider",
                "credentialRef", SECRET_REF));
    }

    public static PolicyExtensionCapability policyProvider(ToolProvenance.SourceKind sourceKind) {
        return new PolicyExtensionCapability("phase6.extension.policy", POLICY,
                Map.of("sourceKind", sourceKind.name(), "order", 50, "decisionBoundary", "ToolExecutionGateway"));
    }

    public static WorkspaceProviderExtensionCapability workspaceProvider(ToolProvenance.SourceKind sourceKind) {
        return new WorkspaceProviderExtensionCapability("phase6.extension.workspace", WORKSPACE_PROVIDER,
                Set.of("logical-resource"), Map.of("sourceKind", sourceKind.name(), "order", 60,
                "boundary", "WorkspaceGateway", "status", "metadata-only"));
    }

    public static MemoryProviderExtensionCapability memoryProvider(ToolProvenance.SourceKind sourceKind) {
        return new MemoryProviderExtensionCapability("phase6.extension.memory", MEMORY_PROVIDER,
                Set.of("metadata-placeholder"), Map.of("sourceKind", sourceKind.name(), "order", 70,
                "status", "placeholder"));
    }

    private static ToolDescriptor descriptor(String id, ToolProvenance.SourceKind sourceKind, ToolSideEffect sideEffect) {
        return new ToolDescriptor(id, id, "Phase 6 conformance tool",
                new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                        Map.of("type", "object", "additionalProperties", true), Set.of("secret"), 16_384),
                Optional.empty(), new ToolProvenance(sourceKind, SOURCE_ID, id,
                Map.of("extension.sourceKind", sourceKind.name())), "1.0.0", Set.of("tool:read"),
                sideEffect == ToolSideEffect.READ_ONLY ? ToolRiskLevel.LOW : ToolRiskLevel.MEDIUM, sideEffect,
                Duration.ofSeconds(2), Map.of("extension.sourceKind", sourceKind.name()));
    }
}
