package io.github.pi_java.agent.infrastructure.mcp.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record McpServerProperties(
        String id,
        boolean enabled,
        String displayName,
        McpTransportKind transport,
        String baseUrl,
        String endpoint,
        String command,
        List<String> args,
        Map<String, String> envSecretRefs,
        Duration timeout,
        McpAuthProperties auth,
        Map<String, String> metadata
) {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public McpServerProperties {
        id = McpAuthProperties.requireNonBlank(id, "id");
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        transport = transport == null ? McpTransportKind.STREAMABLE_HTTP : transport;
        baseUrl = normalizeOptional(baseUrl);
        endpoint = normalizeOptional(endpoint);
        command = normalizeOptional(command);
        args = List.copyOf(Objects.requireNonNull(args, "args must not be null"));
        envSecretRefs = copyStringMap(envSecretRefs, "envSecretRefs");
        timeout = requirePositive(timeout == null ? DEFAULT_TIMEOUT : timeout, "timeout");
        auth = auth == null ? McpAuthProperties.none() : auth;
        metadata = copyStringMap(metadata, "metadata");
    }

    public static McpServerProperties streamableHttp(
            String id,
            String displayName,
            String baseUrl,
            String endpoint,
            McpAuthProperties auth,
            Map<String, String> metadata
    ) {
        return new McpServerProperties(id, true, displayName, McpTransportKind.STREAMABLE_HTTP, baseUrl, endpoint,
                null, List.of(), Map.of(), DEFAULT_TIMEOUT, auth, metadata);
    }

    public static McpServerProperties sse(String id, String displayName, String baseUrl, McpAuthProperties auth, Map<String, String> metadata) {
        return new McpServerProperties(id, true, displayName, McpTransportKind.SSE, baseUrl, null,
                null, List.of(), Map.of(), DEFAULT_TIMEOUT, auth, metadata);
    }

    public static McpServerProperties stdio(
            String id,
            String displayName,
            String command,
            List<String> args,
            Map<String, String> envSecretRefs,
            Duration timeout,
            Map<String, String> metadata
    ) {
        return new McpServerProperties(id, true, displayName, McpTransportKind.STDIO, null, null,
                command, args, envSecretRefs, timeout, McpAuthProperties.none(), metadata);
    }

    public Map<String, String> publicSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("id", id);
        summary.put("displayName", displayName);
        summary.put("enabled", Boolean.toString(enabled));
        summary.put("transport", transport.name());
        summary.put("baseUrl", baseUrl == null ? "" : baseUrl);
        summary.put("endpoint", endpoint == null ? "" : endpoint);
        summary.put("command", command == null ? "" : command);
        summary.put("args", Integer.toString(args.size()));
        summary.put("envRefs", Integer.toString(envSecretRefs.size()));
        summary.put("timeout", timeout.toString());
        summary.putAll(auth.publicSummary());
        summary.put("metadata", Integer.toString(metadata.size()));
        return Map.copyOf(summary);
    }

    @Override
    public String toString() {
        return "McpServerProperties[id=" + id
                + ", enabled=" + enabled
                + ", displayName=" + displayName
                + ", transport=" + transport
                + ", baseUrl=" + baseUrl
                + ", endpoint=" + endpoint
                + ", command=" + command
                + ", args=" + args.size()
                + ", envRefs=" + envSecretRefs.size()
                + ", auth=" + auth.authKind()
                + ", headers=" + auth.customHeaderSecretRefs().size()
                + "]";
    }

    private static Map<String, String> copyStringMap(Map<String, String> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(McpAuthProperties.requireNonBlank(key, name + " key"), Objects.requireNonNull(value, name + " value must not be null")));
        return Map.copyOf(copy);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
