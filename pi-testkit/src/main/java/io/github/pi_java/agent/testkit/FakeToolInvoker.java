package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolInvoker;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FakeToolInvoker implements ToolInvoker {
    private final Map<String, ToolResult> resultsByToolName = new HashMap<>();
    private final List<ToolCall> invocations = new ArrayList<>();

    public FakeToolInvoker register(String toolName, ToolResult result) {
        resultsByToolName.put(toolName, result);
        return this;
    }

    @Override
    public ToolResult invoke(ToolCall toolCall, RunContext context, CancellationToken cancellationToken) {
        invocations.add(toolCall);
        ToolResult result = resultsByToolName.get(toolCall.toolName());
        if (result == null) {
            throw new IllegalArgumentException("no fake result for tool " + toolCall.toolName());
        }
        return result;
    }

    public List<ToolCall> invocations() {
        return List.copyOf(invocations);
    }

    public List<String> registeredToolNames() {
        return List.copyOf(resultsByToolName.keySet());
    }
}
