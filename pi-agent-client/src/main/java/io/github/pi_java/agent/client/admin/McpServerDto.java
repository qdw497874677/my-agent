package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record McpServerDto(
        String serverId,
        String name,
        boolean enabled,
        String transport,
        String authSummary,
        String connectionStatus,
        String discoveryStatus,
        int toolCount,
        Instant lastRefreshedAt,
        String redactedError,
        List<McpToolDto> tools,
        Map<String, String> metadata
) {
    public McpServerDto {
        serverId = requireNonBlank(serverId, "serverId");
        name = requireNonBlank(name, "name");
        transport = requireNonBlank(transport, "transport");
        authSummary = authSummary == null ? "" : authSummary;
        connectionStatus = requireNonBlank(connectionStatus, "connectionStatus");
        discoveryStatus = requireNonBlank(discoveryStatus, "discoveryStatus");
        if (toolCount < 0) {
            throw new IllegalArgumentException("toolCount must not be negative");
        }
        redactedError = redactedError == null ? "" : redactedError;
        tools = List.copyOf(tools == null ? List.of() : tools);
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
