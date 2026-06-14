package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public final class OpenAiProviderErrorMapper {
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[^\\s,;]+");
    private static final Pattern OPENAI_KEY = Pattern.compile("sk-[A-Za-z0-9_\\-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTH_HEADER = Pattern.compile("(?i)authorization\\s*[:=]\\s*[^\\s,;]+(?:\\s+[^\\s,;]+)?");

    private OpenAiProviderErrorMapper() {
    }

    public static ProviderErrorSummary fromHttpStatus(int status, String message, String configuredSecret) {
        return switch (status) {
            case 401, 403 -> summary("provider_authentication_failed", sanitize("Provider authentication failed. " + nullToEmpty(message), configuredSecret), status, false, false, true);
            case 429 -> summary("provider_rate_limited", sanitize("Provider rate limit exceeded. " + nullToEmpty(message), configuredSecret), status, true, true, false);
            case 400 -> fromProviderMessage(message, configuredSecret, status);
            default -> {
                if (status >= 500 && status <= 599) {
                    yield summary("provider_transient_failure", sanitize("Provider transient failure. " + nullToEmpty(message), configuredSecret), status, true, true, false);
                }
                yield fromProviderMessage(message, configuredSecret, status);
            }
        };
    }

    public static ProviderErrorSummary fromProviderMessage(String message, String configuredSecret) {
        return fromProviderMessage(message, configuredSecret, null);
    }

    public static ProviderErrorSummary fromThrowable(Throwable throwable, String configuredSecret, boolean retryableBeforeStream) {
        String safe = sanitize(throwable == null ? "Provider call failed" : throwable.getMessage(), configuredSecret);
        if (safe.toLowerCase().contains("timeout")) {
            return summary("provider_timeout", safe, null, retryableBeforeStream, true, false);
        }
        return summary(retryableBeforeStream ? "provider_transient_failure" : "provider_stream_failed", safe, null,
                retryableBeforeStream, true, false);
    }

    public static ProviderErrorSummary timeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        return summary("provider_timeout", "Provider request timed out after " + timeout.toMillis() + "ms", null, true, true, false);
    }

    public static ProviderErrorSummary cancelled(String reason) {
        return summary("provider_cancelled", sanitize("Provider request cancelled: " + nullToEmpty(reason), null), null, false, true, false);
    }

    public static String sanitize(String message, String configuredSecret) {
        String sanitized = message == null || message.isBlank() ? "Provider error" : message;
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            sanitized = sanitized.replace(configuredSecret, "[REDACTED]");
        }
        sanitized = AUTH_HEADER.matcher(sanitized).replaceAll("[REDACTED_AUTHORIZATION]");
        sanitized = OPENAI_KEY.matcher(sanitized).replaceAll("[REDACTED_SECRET]");
        sanitized = BEARER.matcher(sanitized).replaceAll("[REDACTED_BEARER]");
        sanitized = sanitized.replaceAll("(?i)authorization", "auth");
        return sanitized.isBlank() ? "Provider error" : sanitized;
    }

    static ProviderErrorSummary invalidToolCallArguments(String message) {
        return summary("tool_call_arguments_invalid", sanitize(message, null), null, false, true, false);
    }

    static ProviderErrorSummary incompleteToolCallArguments(String message) {
        return summary("tool_call_arguments_incomplete", sanitize(message, null), null, false, true, false);
    }

    private static ProviderErrorSummary fromProviderMessage(String message, String configuredSecret, Integer status) {
        String safe = sanitize(message, configuredSecret);
        String lower = safe.toLowerCase();
        if (lower.contains("context") && (lower.contains("length") || lower.contains("window") || lower.contains("token"))) {
            return summary("context_length_exceeded", safe, status, false, true, false);
        }
        if (lower.contains("safety") || lower.contains("content filter") || lower.contains("filtered")) {
            return summary("safety_filtered", safe, status, false, true, false);
        }
        if (lower.contains("malformed") || lower.contains("bad response") || lower.contains("invalid response")) {
            return summary("provider_bad_response", safe, status, false, true, false);
        }
        return summary("provider_bad_request", safe, status, false, true, false);
    }

    private static ProviderErrorSummary summary(String code, String message, Integer status,
                                                boolean retryable, boolean recoverable, boolean userActionRequired) {
        PiError error = new PiError(PiError.Category.MODEL, code, PiError.Severity.ERROR,
                userActionRequired ? EventVisibility.USER : EventVisibility.ADMIN,
                retryable, recoverable, userActionRequired);
        return new ProviderErrorSummary(error, code, message, status, retryable, recoverable, userActionRequired);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
