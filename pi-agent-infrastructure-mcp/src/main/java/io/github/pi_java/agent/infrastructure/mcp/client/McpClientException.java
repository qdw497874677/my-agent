package io.github.pi_java.agent.infrastructure.mcp.client;

import java.util.Objects;

public final class McpClientException extends RuntimeException {
    private final String serverId;
    private final McpClientErrorSanitizer.Category category;

    McpClientException(String serverId, McpClientErrorSanitizer.Category category, String message, Throwable cause) {
        super(message, cause);
        this.serverId = Objects.requireNonNull(serverId, "serverId must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public String serverId() {
        return serverId;
    }

    public McpClientErrorSanitizer.Category category() {
        return category;
    }
}
