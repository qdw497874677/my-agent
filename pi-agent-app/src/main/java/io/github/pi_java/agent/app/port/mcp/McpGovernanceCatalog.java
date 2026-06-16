package io.github.pi_java.agent.app.port.mcp;

import java.util.List;
import java.util.Map;

public interface McpGovernanceCatalog {

    List<McpServerStatus> servers();

    McpRefreshStatus refresh();

    default String overallStatus() {
        List<McpServerStatus> statuses = servers();
        if (statuses.isEmpty()) {
            return "UNCONFIGURED";
        }
        boolean anyAuthFailed = statuses.stream().anyMatch(server -> "AUTH_FAILED".equals(server.connectionStatus()));
        if (anyAuthFailed) {
            return "AUTH_FAILED";
        }
        boolean anyDiscoveryFailed = statuses.stream()
                .anyMatch(server -> "DISCOVERY_FAILED".equals(server.discoveryStatus()));
        if (anyDiscoveryFailed) {
            return "DISCOVERY_FAILED";
        }
        boolean anyUnavailable = statuses.stream()
                .anyMatch(server -> !server.enabled() || "UNAVAILABLE".equals(server.connectionStatus()));
        return anyUnavailable ? "UNAVAILABLE" : "HEALTHY";
    }

    final class EmptyMcpGovernanceCatalog implements McpGovernanceCatalog {
        @Override
        public List<McpServerStatus> servers() {
            return List.of();
        }

        @Override
        public McpRefreshStatus refresh() {
            return new McpRefreshStatus(false, 0, 0, 0, "UNCONFIGURED", "", Map.of("surface", "read-only"));
        }
    }
}
