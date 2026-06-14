package io.github.pi_java.agent.app.port.model;

import io.github.pi_java.agent.domain.model.SecretRef;

import java.util.Objects;

public record ResolvedSecret(SecretRef secretRef, String rawValue, String redactedDisplay) {
    public ResolvedSecret {
        Objects.requireNonNull(secretRef, "secretRef must not be null");
        rawValue = requireRawValue(rawValue);
        redactedDisplay = requireSafeDisplay(secretRef, redactedDisplay, rawValue);
    }

    public static ResolvedSecret sensitive(SecretRef secretRef, String rawValue) {
        return new ResolvedSecret(secretRef, rawValue, secretRef.redacted());
    }

    @Override
    public String toString() {
        return "ResolvedSecret[ref=" + redactedDisplay + "]";
    }

    private static String requireRawValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("rawValue must not be blank");
        }
        return rawValue;
    }

    private static String requireSafeDisplay(SecretRef secretRef, String redactedDisplay, String rawValue) {
        if (redactedDisplay == null || redactedDisplay.isBlank()) {
            throw new IllegalArgumentException("redactedDisplay must not be blank");
        }
        if (redactedDisplay.contains(rawValue) || redactedDisplay.contains(secretRef.ref())) {
            throw new IllegalArgumentException("rawValue must be marked sensitive and must not appear in redactedDisplay");
        }
        return redactedDisplay;
    }
}
