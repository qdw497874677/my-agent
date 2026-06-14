package io.github.pi_java.agent.adapter.web.security;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record PiPrincipal(String tenantId, String userId, Collection<String> authorities) {

    public PiPrincipal {
        requireText(tenantId, "tenantId");
        requireText(userId, "userId");
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public RequestContext toRequestContext(String traceId, String correlationId, String causationId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.copyOf(authorities)),
                new CorrelationContext(traceId, correlationId, causationId));
    }

    public Set<String> authoritySet() {
        return authorities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
