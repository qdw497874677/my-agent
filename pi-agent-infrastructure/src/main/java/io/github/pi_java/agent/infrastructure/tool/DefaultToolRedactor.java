package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class DefaultToolRedactor implements ToolRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern SENSITIVE_KEY = Pattern.compile("(?i).*(secret|token|password|credential|api[-_]?key|access[-_]?key|private[-_]?key).*");
    private static final Pattern SENSITIVE_VALUE = Pattern.compile("(?i).*(secretref:|credentialref:|sk-[a-z0-9_-]{8,}|bearer\\s+[a-z0-9._-]{8,}|password=|token=).*");

    private final int maxStringLength;

    public DefaultToolRedactor() {
        this(512);
    }

    public DefaultToolRedactor(int maxStringLength) {
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength must be positive");
        }
        this.maxStringLength = maxStringLength;
    }

    @Override
    public RedactedToolPayload redact(ToolDescriptor descriptor, Map<String, Object> payload) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        LinkedHashSet<String> redactedFields = new LinkedHashSet<>();
        boolean[] truncated = new boolean[]{false};
        Map<String, Object> summary = redactMap(payload, "", descriptor.inputSchema().sensitiveFields(), redactedFields, truncated);
        return new RedactedToolPayload(summary, redactedFields, truncated[0]);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> redactMap(Map<String, Object> payload, String prefix, Set<String> sensitiveFields,
                                          Set<String> redactedFields, boolean[] truncated) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String path = prefix.isBlank() ? key : prefix + "." + key;
            if (isSensitive(path, key, entry.getValue(), sensitiveFields)) {
                result.put(key, REDACTED);
                redactedFields.add(path);
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                result.put(key, redactMap((Map<String, Object>) nested, path, sensitiveFields, redactedFields, truncated));
            } else if (value instanceof List<?> list) {
                result.put(key, redactList(list, path, sensitiveFields, redactedFields, truncated));
            } else if (value instanceof String stringValue) {
                result.put(key, redactString(stringValue, path, redactedFields, truncated));
            } else {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }

    private List<Object> redactList(List<?> list, String prefix, Set<String> sensitiveFields,
                                    Set<String> redactedFields, boolean[] truncated) {
        java.util.ArrayList<Object> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            String path = prefix + "[" + i + "]";
            if (isSensitive(path, prefix, value, sensitiveFields)) {
                result.add(REDACTED);
                redactedFields.add(path);
            } else if (value instanceof String stringValue) {
                result.add(redactString(stringValue, path, redactedFields, truncated));
            } else {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private Object redactString(String value, String path, Set<String> redactedFields, boolean[] truncated) {
        if (SENSITIVE_VALUE.matcher(value).matches()) {
            redactedFields.add(path);
            return REDACTED;
        }
        if (value.length() > maxStringLength) {
            truncated[0] = true;
            return value.substring(0, maxStringLength) + "...";
        }
        return value;
    }

    private boolean isSensitive(String path, String key, Object value, Set<String> sensitiveFields) {
        return sensitiveFields.contains(path)
                || sensitiveFields.contains(key)
                || SENSITIVE_KEY.matcher(key).matches()
                || (value instanceof String stringValue && SENSITIVE_VALUE.matcher(stringValue).matches());
    }
}
