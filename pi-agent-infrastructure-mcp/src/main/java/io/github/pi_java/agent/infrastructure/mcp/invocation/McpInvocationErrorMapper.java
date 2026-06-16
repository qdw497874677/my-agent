package io.github.pi_java.agent.infrastructure.mcp.invocation;

import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public final class McpInvocationErrorMapper {
    public ToolExecutionResult cancelled(ToolExecutionRequest request, String serverId, String toolName, Duration latency) {
        return result(request, ToolExecutionStatus.CANCELLED, "MCP invocation was cancelled before remote execution.",
                "MCP_INVOCATION_CANCELLED", serverId, toolName, "Retry only if the user still wants this tool call.", latency);
    }

    public ToolExecutionResult failure(ToolExecutionRequest request, String serverId, String toolName, RuntimeException failure, Duration latency) {
        ErrorInfo info = categorize(failure);
        return result(request, info.status(), info.summary(), info.category(), serverId, toolName, info.actionHint(), latency);
    }

    ToolExecutionResult toolError(ToolExecutionRequest request, String serverId, String toolName, Duration latency) {
        return result(request, ToolExecutionStatus.FAILED, "MCP tool returned an error response; remote details were redacted.",
                "MCP_TOOL_ERROR", serverId, toolName, "Inspect MCP server-side logs for the detailed remote error.", latency,
                "remote-error-redacted");
    }

    private ToolExecutionResult result(ToolExecutionRequest request, ToolExecutionStatus status, String summary,
                                       String category, String serverId, String toolName, String actionHint, Duration latency) {
        return result(request, status, summary, category, serverId, toolName, actionHint, latency, "error-redacted");
    }

    private ToolExecutionResult result(ToolExecutionRequest request, ToolExecutionStatus status, String summary,
                                       String category, String serverId, String toolName, String actionHint, Duration latency,
                                       String redactionHint) {
        Objects.requireNonNull(request, "request must not be null");
        return new ToolExecutionResult(request.toolCallId(), request.toolId(), status, summary, category,
                Map.of("mcp.remote", true),
                Map.of(
                        "mcp.serverId", safe(serverId),
                        "mcp.toolName", safe(toolName),
                        "mcp.errorCategory", category,
                        "mcp.actionHint", actionHint,
                        "mcp.redactionHint", redactionHint),
                Set.of("mcp.remoteError", "mcp.credentials", "mcp.headers"), null, latency == null ? Duration.ZERO : latency);
    }

    private static ErrorInfo categorize(RuntimeException failure) {
        Throwable root = rootCause(failure);
        String message = (root.getMessage() == null ? failure.getMessage() : root.getMessage());
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (root instanceof TimeoutException || lower.contains("timeout") || lower.contains("timed out")) {
            return new ErrorInfo(ToolExecutionStatus.TIMED_OUT, "MCP invocation timed out; remote details were redacted.",
                    "MCP_TIMEOUT", "Check MCP server latency and configured timeout before retrying.");
        }
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized")
                || lower.contains("forbidden") || lower.contains("auth")) {
            return new ErrorInfo(ToolExecutionStatus.FAILED, "MCP invocation failed authentication or authorization; details were redacted.",
                    "MCP_AUTH_FAILED", "Check MCP server credentials and authorization scope.");
        }
        if (lower.contains("unavailable") || lower.contains("connection") || lower.contains("connect")) {
            return new ErrorInfo(ToolExecutionStatus.FAILED, "MCP server was unavailable; remote details were redacted.",
                    "MCP_SERVER_UNAVAILABLE", "Check MCP server health and network reachability.");
        }
        return new ErrorInfo(ToolExecutionStatus.FAILED, "MCP invocation failed; remote details were redacted.",
                "MCP_INVOCATION_FAILED", "Inspect MCP server-side logs for the detailed failure.");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ErrorInfo(ToolExecutionStatus status, String summary, String category, String actionHint) {
    }
}
