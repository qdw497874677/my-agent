package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;

import java.util.Objects;
import java.util.regex.Pattern;

public record PluginLifecycleSummary(
        String pluginId,
        ExtensionLifecycleState lifecycleState,
        PluginCompatibilitySummary compatibility,
        boolean nonSandboxWarning,
        String redactedError
) {
    private static final Pattern TOKEN_ASSIGNMENT = Pattern.compile("(?i)(token|secret|password|apikey|api_key)=\\S+");
    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("/(?:[A-Za-z0-9._-]+/)+[A-Za-z0-9._-]+(?:\\.[A-Za-z0-9._-]+)?");

    public PluginLifecycleSummary {
        pluginId = requireNonBlank(pluginId, "pluginId");
        Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        redactedError = redact(redactedError);
    }

    public static PluginLifecycleSummary of(String pluginId,
                                            ExtensionLifecycleState lifecycleState,
                                            PluginCompatibilitySummary compatibility,
                                            boolean nonSandboxWarning) {
        return new PluginLifecycleSummary(pluginId, lifecycleState, compatibility, nonSandboxWarning, "");
    }

    public static PluginLifecycleSummary failed(String pluginId,
                                                ExtensionLifecycleState lifecycleState,
                                                PluginCompatibilitySummary compatibility,
                                                boolean nonSandboxWarning,
                                                String error) {
        return new PluginLifecycleSummary(pluginId, lifecycleState, compatibility, nonSandboxWarning, error);
    }

    private static String redact(String error) {
        if (error == null || error.isBlank()) {
            return "";
        }
        String redacted = TOKEN_ASSIGNMENT.matcher(error).replaceAll("$1=<redacted>");
        return UNIX_ABSOLUTE_PATH.matcher(redacted).replaceAll("<redacted-path>");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
