package io.github.pi_java.agent.app.context;

import java.util.Objects;

public record RequestContext(SecurityPrincipalContext principal, CorrelationContext correlation) {

    public RequestContext {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(correlation, "correlation must not be null");
    }

    public String tenantId() {
        return principal.tenantId();
    }

    public String userId() {
        return principal.userId();
    }

    public String traceId() {
        return correlation.traceId();
    }

    public String correlationId() {
        return correlation.correlationId();
    }

    public String causationId() {
        return correlation.causationId();
    }
}
