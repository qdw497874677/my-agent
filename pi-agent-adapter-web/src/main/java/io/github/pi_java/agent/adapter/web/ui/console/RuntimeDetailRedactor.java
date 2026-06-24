package io.github.pi_java.agent.adapter.web.ui.console;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Conservative runtime detail formatter for mobile timeline cards. */
final class RuntimeDetailRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final int DEFAULT_MAX_CHARS = 160;
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(api[_-]?key|password|secret|token|authorization)\\s*=\\s*[^,}\\]\\s|]+"
    );
    private static final Pattern BEARER_SECRET = Pattern.compile("(?i)bearer\\s+[^,}\\]\\s|]+");
    private static final Pattern OPENAI_LIVE_SECRET = Pattern.compile("(?i)sk-live-[A-Za-z0-9._:-]*");
    private static final Pattern RAW_TOKEN_VALUE = Pattern.compile("(?i)raw-token-value");

    private RuntimeDetailRedactor() {
    }

    static String redact(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String redacted = KEY_VALUE_SECRET.matcher(value).replaceAll(match -> match.group(1) + "=" + REDACTED);
        redacted = BEARER_SECRET.matcher(redacted).replaceAll("bearer " + REDACTED);
        redacted = OPENAI_LIVE_SECRET.matcher(redacted).replaceAll(REDACTED);
        redacted = RAW_TOKEN_VALUE.matcher(redacted).replaceAll(REDACTED);
        return redacted;
    }

    static String stringify(Object value) {
        return redact(stringifyValue(value));
    }

    static String shorten(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int boundedMax = Math.max(8, maxChars);
        String redacted = redact(value.trim());
        if (redacted.length() <= boundedMax) {
            return redacted;
        }
        return redacted.substring(0, boundedMax - 1) + "…";
    }

    static String boundedStringify(Object value) {
        return shorten(stringify(value), DEFAULT_MAX_CHARS);
    }

    private static String stringifyValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> sensitiveKeyRank(entry.getKey())))
                    .map(entry -> stringifyValue(entry.getKey()) + "=" + stringifyValue(entry.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(RuntimeDetailRedactor::stringifyValue).collect(Collectors.joining(", ", "[", "]"));
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            return java.util.stream.IntStream.range(0, length)
                    .mapToObj(index -> stringifyValue(Array.get(value, index)))
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }

    private static int sensitiveKeyRank(Object key) {
        if (key == null) {
            return 1;
        }
        String normalized = String.valueOf(key).toLowerCase();
        return normalized.contains("api")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("authorization") ? 0 : 1;
    }
}
