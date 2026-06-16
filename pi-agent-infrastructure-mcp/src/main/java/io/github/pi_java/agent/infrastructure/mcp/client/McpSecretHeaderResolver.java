package io.github.pi_java.agent.infrastructure.mcp.client;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.domain.model.SecretRef;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class McpSecretHeaderResolver {
    private final SecretLookup secretLookup;

    public McpSecretHeaderResolver(SecretLookup secretLookup) {
        this.secretLookup = Objects.requireNonNull(secretLookup, "secretLookup must not be null");
    }

    public static McpSecretHeaderResolver none() {
        return new McpSecretHeaderResolver(ref -> Optional.empty());
    }

    public static McpSecretHeaderResolver from(SecretLookup secretLookup) {
        return new McpSecretHeaderResolver(secretLookup);
    }

    public ResolvedTransportSecrets resolve(McpServerProperties server) {
        Objects.requireNonNull(server, "server must not be null");
        Map<String, String> headers = new LinkedHashMap<>();
        McpAuthProperties auth = server.auth();
        if (auth.bearerTokenRef() != null) {
            headers.put("Authorization", "Bearer " + resolveRaw(auth.bearerTokenRef()));
        }
        if (auth.apiKeySecretRef() != null) {
            headers.put(auth.apiKeyHeaderName(), resolveRaw(auth.apiKeySecretRef()));
        }
        auth.customHeaderSecretRefs().forEach((header, ref) -> headers.put(header, resolveRaw(ref)));

        Map<String, String> env = new LinkedHashMap<>();
        server.envSecretRefs().forEach((name, ref) -> env.put(name, resolveRaw(ref)));
        return new ResolvedTransportSecrets(headers, env);
    }

    private String resolveRaw(String ref) {
        SecretRef secretRef = SecretRef.of(ref);
        return secretLookup.resolve(secretRef)
                .orElseThrow(() -> new SecretResolutionFailedException(secretRef.redacted()))
                .rawValue();
    }

    @FunctionalInterface
    public interface SecretLookup {
        Optional<ResolvedSecret> resolve(SecretRef secretRef);
    }

    public record ResolvedTransportSecrets(Map<String, String> headers, Map<String, String> env) {
        public ResolvedTransportSecrets {
            headers = Map.copyOf(Objects.requireNonNull(headers, "headers must not be null"));
            env = Map.copyOf(Objects.requireNonNull(env, "env must not be null"));
        }

        public Map<String, String> redactedSummary() {
            return Map.of("headers", Integer.toString(headers.size()), "env", Integer.toString(env.size()));
        }

        @Override
        public String toString() {
            return "ResolvedTransportSecrets[headers=" + headers.size() + ", env=" + env.size() + "]";
        }
    }

    public static final class SecretResolutionFailedException extends RuntimeException {
        public SecretResolutionFailedException(String redactedRef) {
            super("MCP secret could not be resolved for " + redactedRef);
        }
    }
}
