package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class FakeToolRegistry implements ToolRegistry {
    private final Map<String, ToolResolution> toolsById = new LinkedHashMap<>();

    public FakeToolRegistry register(String toolId, ToolExecutorBinding executor) {
        return register(descriptor(toolId), executor);
    }

    public FakeToolRegistry register(ToolDescriptor descriptor, ToolExecutorBinding executor) {
        toolsById.put(descriptor.id(), new ToolResolution(descriptor, executor));
        return this;
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return toolsById.values().stream().map(ToolResolution::descriptor).toList();
    }

    @Override
    public Optional<ToolResolution> resolve(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        return Optional.ofNullable(toolsById.get(toolId));
    }

    public static ToolDescriptor descriptor(String toolId) {
        return new ToolDescriptor(toolId, toolId, "Fake testkit tool " + toolId,
                new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of("type", "object"), Set.of(), 64 * 1024),
                Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.TESTKIT, "pi-testkit", "fake:" + toolId, Map.of()),
                "v1", Set.of("testkit"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of());
    }
}
