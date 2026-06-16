package io.github.pi_java.agent.infrastructure.mcp.registry;

import java.time.Instant;
import java.util.Objects;

public record McpDiscoveryResult(
        boolean success,
        int configuredServers,
        int refreshedServers,
        int failedServers,
        Instant refreshedAt,
        String redactedError
) {
    public McpDiscoveryResult {
        if (configuredServers < 0 || refreshedServers < 0 || failedServers < 0) {
            throw new IllegalArgumentException("server counts must not be negative");
        }
        refreshedAt = Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
        redactedError = redactedError == null ? "" : redactedError;
    }
}
