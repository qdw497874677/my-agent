package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ReadOnlyInfoTool {
    private final Map<String, Object> injectedInfo;

    public ReadOnlyInfoTool(Map<String, Object> injectedInfo) {
        this.injectedInfo = Map.copyOf(injectedInfo);
    }

    public InMemoryToolRegistry.ToolRegistration registration() {
        ToolDescriptor descriptor = new ToolDescriptor(
                "builtin.info",
                "Built-in Safe Info",
                "Returns injected non-secret platform information without host env or file reads.",
                new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of("type", "object", "additionalProperties", false), Set.of(), 2048),
                Optional.empty(),
                provenance("read-only-info"),
                "1.0.0",
                Set.of("tool:read"),
                ToolRiskLevel.LOW,
                ToolSideEffect.READ_ONLY,
                Duration.ofSeconds(2),
                Map.of("category", "read-only-info", "previewRequired", false, "approvalRecommended", false)
        );
        return new InMemoryToolRegistry.ToolRegistration(descriptor, (request, cancellationToken) -> new ToolExecutionResult(
                request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS, "returned injected built-in info",
                Optional.empty(), Map.of(), injectedInfo, Set.of(), Optional.empty(), Duration.ZERO, Optional.of(injectedInfo)
        ));
    }

    static ToolProvenance provenance(String bindingRef) {
        return new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "pi-java-builtins", bindingRef, Map.of("safe", "true"));
    }
}
