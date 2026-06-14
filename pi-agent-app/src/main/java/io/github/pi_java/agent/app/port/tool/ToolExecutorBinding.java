package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;

@FunctionalInterface
public interface ToolExecutorBinding {

    ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken cancellationToken);
}
