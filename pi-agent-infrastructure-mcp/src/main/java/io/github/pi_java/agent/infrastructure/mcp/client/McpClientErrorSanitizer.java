package io.github.pi_java.agent.infrastructure.mcp.client;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class McpClientErrorSanitizer {
    public static McpClientErrorSanitizer defaults() {
        return new McpClientErrorSanitizer();
    }

    public McpClientException sanitize(String serverId, Throwable cause) {
        Category category = categorize(cause);
        String safeServerId = sanitizeServerId(serverId);
        return new McpClientException(safeServerId, category,
                "MCP client initialization failed for " + safeServerId
                        + " [" + category + "]: " + hint(category),
                cause);
    }

    public Category categorize(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof IllegalArgumentException) {
                return Category.CONFIG_INVALID;
            }
            if (current instanceof HttpTimeoutException || current instanceof TimeoutException) {
                return Category.TIMEOUT;
            }
            if (current instanceof ConnectException) {
                return Category.SERVER_UNAVAILABLE;
            }
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("401") || message.contains("403") || message.contains("unauthorized") || message.contains("forbidden")) {
                return Category.AUTH_FAILED;
            }
            if (message.contains("timed out") || message.contains("timeout")) {
                return Category.TIMEOUT;
            }
            if (message.contains("connection refused") || message.contains("unavailable") || message.contains("not found")) {
                return Category.SERVER_UNAVAILABLE;
            }
            if (message.contains("transport")) {
                return Category.TRANSPORT_ERROR;
            }
            current = current.getCause();
        }
        return Category.UNKNOWN;
    }

    private static String hint(Category category) {
        return switch (category) {
            case CONFIG_INVALID -> "Review MCP server configuration.";
            case AUTH_FAILED -> "Check MCP server credentials.";
            case SERVER_UNAVAILABLE -> "Check MCP server availability and network access.";
            case TIMEOUT -> "Check MCP server latency or increase the configured timeout.";
            case TRANSPORT_ERROR -> "Check MCP transport configuration.";
            case UNKNOWN -> "Inspect server health using sanitized infrastructure logs.";
        };
    }

    private static String sanitizeServerId(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return "<unknown>";
        }
        return serverId.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    public enum Category {
        CONFIG_INVALID,
        AUTH_FAILED,
        SERVER_UNAVAILABLE,
        TIMEOUT,
        TRANSPORT_ERROR,
        UNKNOWN
    }
}
