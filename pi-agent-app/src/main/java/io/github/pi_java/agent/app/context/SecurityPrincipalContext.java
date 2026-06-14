package io.github.pi_java.agent.app.context;

import java.util.Set;

public record SecurityPrincipalContext(String tenantId, String userId, Set<String> authorities) {

    public SecurityPrincipalContext {
        requireText(tenantId, "tenantId");
        requireText(userId, "userId");
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
