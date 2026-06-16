package io.github.pi_java.agent.infrastructure.mcp.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpSafetyValidator {
    private McpSafetyValidator() {
    }

    public static ValidationResult validate(McpServerProperties server) {
        Objects.requireNonNull(server, "server must not be null");
        validateTransport(server);
        validateAuthRefs(server.auth());
        validateEnvRefs(server.envSecretRefs());
        return new ValidationResult(true, List.of(), server.publicSummary());
    }

    private static void validateTransport(McpServerProperties server) {
        switch (server.transport()) {
            case STREAMABLE_HTTP -> {
                validateHttpUrl(server.baseUrl(), server.id(), "baseUrl");
                if (server.endpoint() != null && !server.endpoint().startsWith("/")) {
                    fail(server.id(), "endpoint must start with /");
                }
            }
            case SSE -> validateHttpUrl(server.baseUrl(), server.id(), "baseUrl");
            case STDIO -> {
                if (server.command() == null || server.command().isBlank()) {
                    fail(server.id(), "stdio command must be configured for STDIO transport");
                }
                if (server.baseUrl() != null) {
                    fail(server.id(), "baseUrl is not allowed for STDIO transport");
                }
            }
        }
    }

    private static void validateHttpUrl(String value, String serverId, String field) {
        if (value == null || value.isBlank()) {
            fail(serverId, field + " must be configured for HTTP/SSE transport");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException ex) {
            fail(serverId, field + " must be a valid URI");
            return;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            fail(serverId, "unsupported URL scheme for MCP server; only http and https are allowed");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            fail(serverId, "URL host must not be blank");
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            fail(serverId, "credentials in URL are not allowed");
        }
    }

    private static void validateAuthRefs(McpAuthProperties auth) {
        validateSecretRef(auth.credentialRef(), "credentialRef");
        validateSecretRef(auth.bearerTokenRef(), "bearerTokenRef");
        validateSecretRef(auth.apiKeySecretRef(), "apiKeySecretRef");
        auth.customHeaderSecretRefs().forEach((header, ref) -> validateSecretRef(ref, "customHeaderSecretRefs." + header));
    }

    private static void validateEnvRefs(Map<String, String> envSecretRefs) {
        envSecretRefs.forEach((name, ref) -> validateSecretRef(ref, "envSecretRefs." + name));
    }

    private static void validateSecretRef(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        int separator = value.indexOf(':');
        if (separator <= 0) {
            fail("<unknown>", field + " must be a secret reference using scheme:target syntax");
        }
        String scheme = value.substring(0, separator);
        if (!(scheme.equals("env") || scheme.equals("config") || scheme.equals("vault") || scheme.equals("credential"))) {
            fail("<unknown>", field + " must be a secret reference, not a raw secret value");
        }
        if (looksLikeRawHeaderSecret(value)) {
            fail("<unknown>", field + " must be a secret reference, not a raw secret value");
        }
    }

    private static boolean looksLikeRawHeaderSecret(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("bearer ")
                || normalized.startsWith("basic ")
                || normalized.contains("sk-")
                || normalized.contains("token=")
                || normalized.contains("password=");
    }

    private static void fail(String serverId, String reason) {
        throw new IllegalArgumentException("Invalid MCP server configuration for " + sanitizeId(serverId) + ": " + reason);
    }

    private static String sanitizeId(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return "<unknown>";
        }
        return serverId.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    public record ValidationResult(boolean valid, List<String> warnings, Map<String, String> sanitizedSummary) {
        public ValidationResult {
            warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings must not be null"));
            sanitizedSummary = Map.copyOf(Objects.requireNonNull(sanitizedSummary, "sanitizedSummary must not be null"));
        }
    }
}
