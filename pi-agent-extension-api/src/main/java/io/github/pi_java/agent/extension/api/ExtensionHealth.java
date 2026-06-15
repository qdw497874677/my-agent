package io.github.pi_java.agent.extension.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ExtensionHealth(
        Status status,
        String summary,
        Instant checkedAt,
        Map<String, Object> redactedDetails
) {
    public ExtensionHealth {
        Objects.requireNonNull(status, "status must not be null");
        summary = Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        redactedDetails = Map.copyOf(Objects.requireNonNull(redactedDetails, "redactedDetails must not be null"));
    }

    public static ExtensionHealth up(String summary) {
        return new ExtensionHealth(Status.UP, summary, Instant.EPOCH, Map.of());
    }

    public static ExtensionHealth down(String summary) {
        return new ExtensionHealth(Status.DOWN, summary, Instant.EPOCH, Map.of());
    }

    public enum Status {
        UP,
        DEGRADED,
        DOWN,
        UNKNOWN
    }
}
