package io.github.pi_java.agent.infrastructure.mcp.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record McpAuthProperties(
        String credentialRef,
        String bearerTokenRef,
        String apiKeyHeaderName,
        String apiKeySecretRef,
        Map<String, String> customHeaderSecretRefs
) {
    public McpAuthProperties {
        credentialRef = normalizeOptionalRef(credentialRef, "credentialRef");
        bearerTokenRef = normalizeOptionalRef(bearerTokenRef, "bearerTokenRef");
        apiKeyHeaderName = normalizeOptional(apiKeyHeaderName);
        apiKeySecretRef = normalizeOptionalRef(apiKeySecretRef, "apiKeySecretRef");
        if (apiKeySecretRef != null && apiKeyHeaderName == null) {
            throw new IllegalArgumentException("apiKeyHeaderName must not be blank when apiKeySecretRef is configured");
        }
        customHeaderSecretRefs = copyRefs(customHeaderSecretRefs, "customHeaderSecretRefs");
    }

    public static McpAuthProperties none() {
        return new McpAuthProperties(null, null, null, null, Map.of());
    }

    public static McpAuthProperties credentialRef(String credentialRef) {
        return new McpAuthProperties(credentialRef, null, null, null, Map.of());
    }

    public static McpAuthProperties bearerTokenRef(String bearerTokenRef) {
        return new McpAuthProperties(null, bearerTokenRef, null, null, Map.of());
    }

    public static McpAuthProperties apiKeyHeaderRef(String headerName, String secretRef) {
        return new McpAuthProperties(null, null, headerName, secretRef, Map.of(headerName, secretRef));
    }

    public McpAuthProperties withCustomHeaderSecretRefs(Map<String, String> headerSecretRefs) {
        return new McpAuthProperties(credentialRef, bearerTokenRef, apiKeyHeaderName, apiKeySecretRef, headerSecretRefs);
    }

    public Map<String, String> publicSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("auth", authKind());
        summary.put("credentialRef", redactedRef(credentialRef));
        summary.put("bearerTokenRef", redactedRef(bearerTokenRef));
        summary.put("apiKeyHeader", apiKeyHeaderName == null ? "" : apiKeyHeaderName);
        summary.put("apiKeySecretRef", redactedRef(apiKeySecretRef));
        summary.put("headers", Integer.toString(customHeaderSecretRefs.size()));
        return Map.copyOf(summary);
    }

    public String authKind() {
        if (credentialRef != null) {
            return "credential-ref";
        }
        if (bearerTokenRef != null) {
            return "bearer-token-ref";
        }
        if (apiKeySecretRef != null) {
            return "api-key-ref";
        }
        if (!customHeaderSecretRefs.isEmpty()) {
            return "custom-header-refs";
        }
        return "none";
    }

    public static String redactedRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return "";
        }
        int separator = ref.indexOf(':');
        if (separator <= 0) {
            return "***";
        }
        return ref.substring(0, separator) + ":***";
    }

    private static Map<String, String> copyRefs(Map<String, String> refs, String name) {
        Objects.requireNonNull(refs, name + " must not be null");
        Map<String, String> copy = new LinkedHashMap<>();
        refs.forEach((header, ref) -> copy.put(requireNonBlank(header, "headerName"), requireNonBlank(ref, name + "." + header)));
        return Map.copyOf(copy);
    }

    private static String normalizeOptionalRef(String value, String name) {
        String normalized = normalizeOptional(value);
        if (normalized != null && normalized.indexOf(':') <= 0) {
            throw new IllegalArgumentException(name + " must use scheme:target syntax");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
