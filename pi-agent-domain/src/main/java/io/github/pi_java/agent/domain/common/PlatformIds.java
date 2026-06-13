package io.github.pi_java.agent.domain.common;

/**
 * Platform-wide typed identifiers for tenant, user, runtime, workspace, and tracing context.
 */
public final class PlatformIds {

    private PlatformIds() {
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public record TenantId(String value) {
        public TenantId {
            value = requireNonBlank(value, "tenantId");
        }
    }

    public record UserId(String value) {
        public UserId {
            value = requireNonBlank(value, "userId");
        }
    }

    public record AgentId(String value) {
        public AgentId {
            value = requireNonBlank(value, "agentId");
        }
    }

    public record SessionId(String value) {
        public SessionId {
            value = requireNonBlank(value, "sessionId");
        }
    }

    public record RunId(String value) {
        public RunId {
            value = requireNonBlank(value, "runId");
        }
    }

    public record StepId(String value) {
        public StepId {
            value = requireNonBlank(value, "stepId");
        }
    }

    public record WorkspaceId(String value) {
        public WorkspaceId {
            value = requireNonBlank(value, "workspaceId");
        }
    }

    public record TraceId(String value) {
        public TraceId {
            value = requireNonBlank(value, "traceId");
        }
    }

    public record CorrelationId(String value) {
        public CorrelationId {
            value = requireNonBlank(value, "correlationId");
        }
    }

    public record CausationId(String value) {
        public CausationId {
            value = requireNonBlank(value, "causationId");
        }
    }
}
