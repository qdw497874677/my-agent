package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.util.List;
import java.util.Objects;

public record ModelRequest(RunContext context, List<ToolResult> toolResults) {
    public ModelRequest {
        Objects.requireNonNull(context, "context must not be null");
        toolResults = List.copyOf(Objects.requireNonNull(toolResults, "toolResults must not be null"));
    }
}
