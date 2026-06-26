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
    private static final Pattern API_KEY_ASSIGNMENT = Pattern.compile("(?i)api[_-]?key\\s*[:=]\\s*[^\\s,;]+");

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
        if (throwable != null) {
            Integer httpStatus = extractHttpStatus(throwable);
            if (httpStatus != null && retryableBeforeStream) {
                return fromHttpStatus(httpStatus, throwable.getMessage(), configuredSecret);
            }
        }
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
        sanitized = API_KEY_ASSIGNMENT.matcher(sanitized).replaceAll("[REDACTED_API_KEY]");
        sanitized = OPENAI_KEY.matcher(sanitized).replaceAll("[REDACTED_SECRET]");
        sanitized = BEARER.matcher(sanitized).replaceAll("[REDACTED_BEARER]");
        sanitized = sanitized.replaceAll("(?i)authorization", "auth");
        sanitized = sanitized.replaceAll("(?i)api[_-]?key", "api credential");
        return sanitized.isBlank() ? "Provider error" : sanitized;
    }

    private static Integer extractHttpStatus(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 16) {
            Integer status = extractHttpStatusFromSingleThrowable(current);
            if (status != null) {
                return status;
            }
            current = current.getCause();
        }
        return null;
    }

    private static Integer extractHttpStatusFromSingleThrowable(Throwable throwable) {
        Integer status = invokeStatusLikeMethod(throwable, "status");
        if (status != null) {
            return status;
        }
        status = invokeStatusLikeMethod(throwable, "getRawStatusCode");
        if (status != null) {
            return status;
        }
        Object statusCode = invokeNoArgMethod(throwable, "getStatusCode");
        return extractStatusValue(statusCode);
    }

    private static Integer invokeStatusLikeMethod(Object target, String methodName) {
        Object value = invokeNoArgMethod(target, methodName);
        return extractStatusValue(value);
    }

    private static Object invokeNoArgMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Keep provider SDK details optional; most HTTP exceptions do not expose a common type.
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Integer extractStatusValue(Object value) {
        if (value instanceof Number number) {
            return validStatus(number.intValue());
        }
        Object nestedValue = invokeNoArgMethod(value, "value");
        if (nestedValue instanceof Number number) {
            return validStatus(number.intValue());
        }
        return null;
    }

    private static Integer validStatus(int status) {
        return status >= 100 && status <= 599 ? status : null;
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
