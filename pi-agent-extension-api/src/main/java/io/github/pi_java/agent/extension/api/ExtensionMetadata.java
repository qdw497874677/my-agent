package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;

public record ExtensionMetadata(
        String extensionId,
        String name,
        String version,
        String vendor,
        ExtensionApiVersion apiVersion,
        ExtensionCompatibility compatibility,
        ExtensionLifecycleState lifecycleState,
        ExtensionHealth health,
        boolean enabled,
        Map<String, Object> redactedMetadata
) {
    public ExtensionMetadata {
        extensionId = ExtensionStrings.requireNonBlank(extensionId, "extensionId");
        name = ExtensionStrings.requireNonBlank(name, "name");
        version = ExtensionStrings.requireNonBlank(version, "version");
        vendor = ExtensionStrings.requireNonBlank(vendor, "vendor");
        Objects.requireNonNull(apiVersion, "apiVersion must not be null");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
        Objects.requireNonNull(health, "health must not be null");
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }
}
