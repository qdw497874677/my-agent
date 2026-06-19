package io.github.pi_java.agent.domain.common;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Platform-wide typed identifiers for tenant, user, runtime, workspace, and tracing context.
 */
public final class PlatformIds {

    private static final Pattern W3C_TRACE_ID_PATTERN = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern LEGACY_UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String ZERO_TRACE_ID = "00000000000000000000000000000000";

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
            if (!W3C_TRACE_ID_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("traceId must be a W3C-compatible 32-character lowercase hex value");
            }
            if (ZERO_TRACE_ID.equals(value)) {
                throw new IllegalArgumentException("traceId must not be the all-zero W3C trace id");
            }
        }

        public static TraceId newRandom() {
            UUID uuid = UUID.randomUUID();
            return new TraceId("%016x%016x".formatted(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
        }

        public static TraceId fromLegacyUuid(String legacy) {
            String value = requireNonBlank(legacy, "traceId").trim();
            if (LEGACY_UUID_PATTERN.matcher(value).matches()) {
                value = value.replace("-", "").toLowerCase();
            }
            return new TraceId(value);
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
