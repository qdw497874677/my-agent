package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OperationsSummaryResponse(
        List<OperationMetricDto> runs,
        List<OperationMetricDto> models,
        List<OperationMetricDto> tools,
        List<OperationMetricDto> policies,
        List<OperationMetricDto> mcp,
        List<OperationMetricDto> plugins,
        List<OperationMetricDto> errors,
        List<OperationalWarningDto> warnings,
        Instant generatedAt
) {
    public OperationsSummaryResponse {
        runs = List.copyOf(Objects.requireNonNull(runs, "runs must not be null"));
        models = List.copyOf(Objects.requireNonNull(models, "models must not be null"));
        tools = List.copyOf(Objects.requireNonNull(tools, "tools must not be null"));
        policies = List.copyOf(Objects.requireNonNull(policies, "policies must not be null"));
        mcp = List.copyOf(Objects.requireNonNull(mcp, "mcp must not be null"));
        plugins = List.copyOf(Objects.requireNonNull(plugins, "plugins must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    }
}
