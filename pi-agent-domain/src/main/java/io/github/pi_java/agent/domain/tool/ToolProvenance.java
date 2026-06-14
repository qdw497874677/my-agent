package io.github.pi_java.agent.domain.tool;

import java.util.Map;
import java.util.Objects;

public record ToolProvenance(
        SourceKind sourceKind,
        String sourceId,
        String bindingRef,
        Map<String, String> metadata
) {
    public ToolProvenance {
        Objects.requireNonNull(sourceKind, "sourceKind must not be null");
        sourceId = ToolValidation.requireNonBlank(sourceId, "sourceId");
        bindingRef = ToolValidation.requireNonBlank(bindingRef, "bindingRef");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    public enum SourceKind {
        BUILT_IN,
        TESTKIT,
        SPI,
        SPRING_BEAN,
        MCP,
        PLUGIN,
        REMOTE
    }
}
