package io.github.pi_java.agent.app.port.plugin;

import java.util.Map;
import java.util.Objects;

public record PluginMutationStatus(
        boolean applied,
        String pluginId,
        String operation,
        String previousLifecycleStatus,
        String resultingLifecycleStatus,
        String status,
        String redactedError,
        Map<String, String> metadata
) {
    public PluginMutationStatus {
        pluginId = pluginId == null ? "" : pluginId;
        operation = requireNonBlank(operation, "operation");
        previousLifecycleStatus = previousLifecycleStatus == null ? "" : previousLifecycleStatus;
        resultingLifecycleStatus = resultingLifecycleStatus == null ? "" : resultingLifecycleStatus;
        status = requireNonBlank(status, "status");
        redactedError = redactedError == null ? "" : redactedError;
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
