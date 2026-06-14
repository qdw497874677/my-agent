package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class FakeToolExecutorBinding implements ToolExecutorBinding {
    private final FakeToolInvoker toolInvoker;
    private final RunContext context;

    public FakeToolExecutorBinding(FakeToolInvoker toolInvoker, RunContext context) {
        this.toolInvoker = Objects.requireNonNull(toolInvoker, "toolInvoker must not be null");
        this.context = context;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request, CancellationToken cancellationToken) {
        Objects.requireNonNull(request, "request must not be null");
        ToolCall toolCall = new ToolCall(request.toolCallId(), request.runId(), request.stepId(), request.toolId(),
                request.arguments(), request.requestedAt());
        Instant startedAt = Instant.now();
        ToolResult toolResult = toolInvoker.invoke(toolCall, context, cancellationToken);
        ToolExecutionStatus status = toolResult.success() ? ToolExecutionStatus.SUCCESS : ToolExecutionStatus.FAILED;
        String errorCategory = toolResult.optionalError().map(error -> error.category().name()).orElse(null);
        return new ToolExecutionResult(request.toolCallId(), request.toolId(), status, toolResult.summary(),
                Optional.ofNullable(errorCategory), Map.of(), Map.of("summary", toolResult.summary()), Set.of(),
                Optional.empty(), Duration.between(startedAt, Instant.now()).abs(),
                Optional.of(Map.of("summary", toolResult.summary(), "success", toolResult.success())));
    }
}
