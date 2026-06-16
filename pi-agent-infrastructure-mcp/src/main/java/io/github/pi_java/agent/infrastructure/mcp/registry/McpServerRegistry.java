package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.infrastructure.mcp.client.McpClientErrorSanitizer;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientException;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientFactory;
import io.github.pi_java.agent.infrastructure.mcp.client.McpClientHandle;
import io.github.pi_java.agent.infrastructure.mcp.client.McpSecretHeaderResolver;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpServerRegistry {
    private final List<McpServerProperties> servers;
    private final DiscoveryClientFactory clientFactory;
    private final Clock clock;
    private volatile Map<String, ServerSnapshot> snapshots;

    public McpServerRegistry(List<McpServerProperties> servers, McpClientFactory clientFactory, Clock clock) {
        this(servers, server -> new HandleDiscoveryClient(clientFactory.create(server)), clock);
    }

    public McpServerRegistry(List<McpServerProperties> servers, McpSecretHeaderResolver secretResolver, Clock clock) {
        this(servers, new McpClientFactory(secretResolver), clock);
    }

    public McpServerRegistry(List<McpServerProperties> servers, DiscoveryClientFactory clientFactory, Clock clock) {
        this.servers = List.copyOf(Objects.requireNonNull(servers, "servers must not be null"));
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.snapshots = initialSnapshots(this.servers, Instant.EPOCH);
    }

    public synchronized McpDiscoveryResult refresh() {
        Instant refreshedAt = clock.instant();
        Map<String, ServerSnapshot> previous = snapshots;
        Map<String, ServerSnapshot> next = new LinkedHashMap<>();
        int refreshed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (McpServerProperties server : servers) {
            if (!server.enabled()) {
                next.put(server.id(), ServerSnapshot.disabled(server, refreshedAt));
                continue;
            }
            try (DiscoveryClient client = clientFactory.create(server)) {
                List<DiscoveredTool> tools = client.listTools().stream()
                        .map(tool -> DiscoveredTool.available(tool, ""))
                        .sorted(Comparator.comparing(DiscoveredTool::name))
                        .toList();
                next.put(server.id(), new ServerSnapshot(server, "CONNECTED", "DISCOVERED", refreshedAt, "", tools));
                refreshed++;
            } catch (RuntimeException ex) {
                failed++;
                String error = sanitize(server.id(), ex);
                errors.add(error);
                List<DiscoveredTool> unavailable = previous.getOrDefault(server.id(), ServerSnapshot.disabled(server, refreshedAt))
                        .tools().stream()
                        .map(tool -> tool.markUnavailable(error))
                        .toList();
                String connectionStatus = error.startsWith("AUTH_FAILED") ? "AUTH_FAILED"
                        : ex instanceof McpClientException clientException ? clientException.category().name()
                        : "UNAVAILABLE";
                next.put(server.id(), new ServerSnapshot(server, connectionStatus, "DISCOVERY_FAILED", refreshedAt, error, unavailable));
            }
        }
        snapshots = Map.copyOf(next);
        return new McpDiscoveryResult(failed == 0, servers.size(), refreshed, failed, refreshedAt, String.join("; ", errors));
    }

    public List<ServerSnapshot> snapshots() {
        return snapshots.values().stream()
                .sorted(Comparator.comparing(snapshot -> snapshot.server().id()))
                .toList();
    }

    private static Map<String, ServerSnapshot> initialSnapshots(List<McpServerProperties> servers, Instant refreshedAt) {
        Map<String, ServerSnapshot> initial = new LinkedHashMap<>();
        servers.forEach(server -> initial.put(server.id(), server.enabled()
                ? new ServerSnapshot(server, "NOT_CONNECTED", "NOT_DISCOVERED", refreshedAt, "", List.of())
                : ServerSnapshot.disabled(server, refreshedAt)));
        return Map.copyOf(initial);
    }

    private static String sanitize(String serverId, RuntimeException ex) {
        if (ex instanceof McpClientException clientException) {
            return clientException.category().name() + ": " + clientException.getMessage();
        }
        McpClientException sanitized = McpClientErrorSanitizer.defaults().sanitize(serverId, ex);
        return sanitized.category().name() + ": " + sanitized.getMessage();
    }

    public interface DiscoveryClientFactory {
        DiscoveryClient create(McpServerProperties server);
    }

    public interface DiscoveryClient extends AutoCloseable {
        List<McpSchema.Tool> listTools();

        @Override
        void close();
    }

    private record HandleDiscoveryClient(McpClientHandle handle) implements DiscoveryClient {
        private HandleDiscoveryClient {
            Objects.requireNonNull(handle, "handle must not be null");
        }

        @Override
        public List<McpSchema.Tool> listTools() {
            return handle.listTools();
        }

        @Override
        public void close() {
            handle.close();
        }
    }

    public record ServerSnapshot(
            McpServerProperties server,
            String connectionStatus,
            String discoveryStatus,
            Instant lastRefreshedAt,
            String redactedError,
            List<DiscoveredTool> tools
    ) {
        public ServerSnapshot {
            server = Objects.requireNonNull(server, "server must not be null");
            connectionStatus = requireNonBlank(connectionStatus, "connectionStatus");
            discoveryStatus = requireNonBlank(discoveryStatus, "discoveryStatus");
            lastRefreshedAt = Objects.requireNonNull(lastRefreshedAt, "lastRefreshedAt must not be null");
            redactedError = redactedError == null ? "" : redactedError;
            tools = List.copyOf(Objects.requireNonNull(tools, "tools must not be null"));
        }

        static ServerSnapshot disabled(McpServerProperties server, Instant refreshedAt) {
            return new ServerSnapshot(server, "DISABLED", "DISABLED", refreshedAt, "", List.of());
        }
    }

    public record DiscoveredTool(McpSchema.Tool sdkTool, boolean available, String redactedError) {
        public DiscoveredTool {
            sdkTool = Objects.requireNonNull(sdkTool, "sdkTool must not be null");
            redactedError = redactedError == null ? "" : redactedError;
        }

        static DiscoveredTool available(McpSchema.Tool tool, String redactedError) {
            return new DiscoveredTool(tool, true, redactedError);
        }

        DiscoveredTool markUnavailable(String error) {
            return new DiscoveredTool(sdkTool, false, error);
        }

        public String name() {
            return sdkTool.name();
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
