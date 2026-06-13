package io.github.pi_java.agent.domain.tool;

import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;

public interface ToolInvoker {
    ToolResult invoke(ToolCall toolCall, RunContext context, CancellationToken cancellationToken);
}
