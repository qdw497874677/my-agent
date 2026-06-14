package io.github.pi_java.agent.app.context;

public record CorrelationContext(String traceId, String correlationId, String causationId) {

    public CorrelationContext {
        requireText(traceId, "traceId");
        requireText(correlationId, "correlationId");
        if (causationId != null && causationId.isBlank()) {
            throw new IllegalArgumentException("causationId must be null or non-blank");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
