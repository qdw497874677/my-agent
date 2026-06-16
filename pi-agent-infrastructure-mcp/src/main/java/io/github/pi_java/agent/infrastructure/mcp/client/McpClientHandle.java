package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;

import java.util.Objects;

public final class McpClientHandle implements AutoCloseable {
    private final String serverId;
    private final McpTransportKind transportKind;
    private final McpClientFactory.InitializedClient client;

    McpClientHandle(String serverId, McpTransportKind transportKind, McpClientFactory.InitializedClient client) {
        this.serverId = requireNonBlank(serverId, "serverId");
        this.transportKind = Objects.requireNonNull(transportKind, "transportKind must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    public String serverId() {
        return serverId;
    }

    public McpTransportKind transportKind() {
        return transportKind;
    }

    McpClientFactory.InitializedClient client() {
        return client;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String toString() {
        return "McpClientHandle[serverId=" + serverId + ", transportKind=" + transportKind + "]";
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
