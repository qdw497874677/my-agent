package io.github.pi_java.agent.infrastructure.observability;

import java.util.Locale;

public final class PiTelemetryRedactor {

    public static final String UNKNOWN = "unknown";
    public static final String REDACTED = "[REDACTED]";
    public static final int MAX_TAG_LENGTH = 80;

    public String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        if (isSensitive(trimmed)) {
            return REDACTED;
        }
        if (trimmed.length() <= MAX_TAG_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TAG_LENGTH);
    }

    public boolean isSensitive(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("secret")
                || lower.contains("password")
                || lower.contains("authorization")
                || lower.contains("bearer")
                || lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("token");
    }
}
