package io.github.pi_java.agent.sample.plugin;

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
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

@Extension
public final class ReadonlySampleExtensionSource implements ExtensionSource, ExtensionPoint {

    public static final String PLUGIN_ID = "sample-readonly-plugin";
    public static final String TOOL_ID = "plugin.sample.readonly.lookup";

    @Override
    public ExtensionMetadata metadata() {
        return new ExtensionMetadata(PLUGIN_ID, "Sample Read-only Plugin", "1.0.0", "Pi Java",
                ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                ExtensionLifecycleState.STARTED, ExtensionHealth.up("sample plugin ready"), true,
                Map.of("sourceKind", "PLUGIN", "sample", "readonly"));
    }

    @Override
    public List<ExtensionCapability> capabilities() {
        return List.of(new ToolExtensionCapability(TOOL_ID, descriptor(), binding(),
                Map.of("sourceKind", "PLUGIN", "version", "1.0.0", "sample", "readonly")));
    }

    private static ToolDescriptor descriptor() {
        ToolSchema input = new ToolSchema("json-schema-2020-12",
                Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")),
                        "required", List.of("query")), Set.of("query"), 4096);
        return new ToolDescriptor(TOOL_ID, "Sample read-only lookup",
                "Deterministic read-only sample plugin tool for controlled PF4J loading tests.", input, Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.PLUGIN, PLUGIN_ID, TOOL_ID,
                        Map.of("plugin.id", PLUGIN_ID, "sample", "readonly")), "1.0.0", Set.of("tool:plugin", "tool:read"),
                ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(2),
                Map.of("plugin.id", PLUGIN_ID, "sample", "readonly"));
    }

    private static io.github.pi_java.agent.app.port.tool.ToolExecutorBinding binding() {
        return (request, cancellationToken) -> {
            if (cancellationToken.isCancellationRequested()) {
                return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.CANCELLED,
                        "PLUGIN_CANCELLED", Optional.of("cancelled"), Map.of("plugin.id", PLUGIN_ID), Map.of(), Set.of(),
                        Optional.empty(), Duration.ZERO, Optional.empty());
            }
            Object query = request.arguments().getOrDefault("query", "");
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "SAMPLE_PLUGIN_READONLY_OK", Optional.empty(),
                    Map.of("plugin.id", PLUGIN_ID, "query", String.valueOf(query), "mode", "read-only"),
                    Map.of("message", "sample plugin read-only result", "query", String.valueOf(query)), Set.of(), Optional.empty(),
                    Duration.ZERO, Optional.of(Map.of("message", "sample plugin read-only result", "query", String.valueOf(query))));
        };
    }
}
