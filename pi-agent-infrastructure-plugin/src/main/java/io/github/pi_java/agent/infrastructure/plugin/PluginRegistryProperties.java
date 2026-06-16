package io.github.pi_java.agent.infrastructure.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PluginRegistryProperties(
        boolean enabled,
        Optional<Path> pluginDirectory,
        boolean loadOnStartup,
        boolean manualRefreshEnabled,
        List<String> allowedPluginIds,
        List<String> selectedPluginIds,
        String platformApiVersion,
        boolean allowDuplicateOverrides,
        boolean nonSandboxWarningAcknowledged
) {
    public PluginRegistryProperties {
        Objects.requireNonNull(pluginDirectory, "pluginDirectory must not be null");
        allowedPluginIds = copyNonBlank(allowedPluginIds, "allowedPluginIds");
        selectedPluginIds = copyNonBlank(selectedPluginIds, "selectedPluginIds");
        platformApiVersion = requireNonBlank(platformApiVersion, "platformApiVersion");
    }

    public static PluginRegistryProperties disabled() {
        return new PluginRegistryProperties(false, Optional.empty(), false, true, List.of(), List.of(), "1.0.0", false, false);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (enabled && pluginDirectory.isEmpty()) {
            errors.add("pluginDirectory is required when controlled plugin loading is enabled");
        }
        if (!allowedPluginIds.isEmpty()) {
            List<String> outsideAllowlist = selectedPluginIds.stream()
                    .filter(id -> !allowedPluginIds.contains(id))
                    .toList();
            if (!outsideAllowlist.isEmpty()) {
                errors.add("selectedPluginIds contains values outside allowedPluginIds: " + outsideAllowlist);
            }
        }
        if (enabled && !nonSandboxWarningAcknowledged) {
            errors.add("nonSandboxWarningAcknowledged must be true because PF4J plugin jars are not a sandbox boundary");
        }
        return List.copyOf(errors);
    }

    public PluginRegistryProperties requireValid() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        return this;
    }

    private static List<String> copyNonBlank(List<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " must not be null");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            unique.add(requireNonBlank(value, fieldName + " entry"));
        }
        return List.copyOf(unique);
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
