package io.github.pi_java.agent.infrastructure.plugin;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record PluginDescriptorSummary(
        String pluginId,
        String name,
        String version,
        String provider,
        String sourcePathSummary,
        PluginCompatibilitySummary compatibility,
        Map<String, String> redactedMetadata
) {
    public PluginDescriptorSummary {
        pluginId = requireNonBlank(pluginId, "pluginId");
        name = requireNonBlank(name, "name");
        version = requireNonBlank(version, "version");
        provider = requireNonBlank(provider, "provider");
        sourcePathSummary = requireNonBlank(sourcePathSummary, "sourcePathSummary");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    public static PluginDescriptorSummary fromControlledPath(
            String pluginId,
            String name,
            String version,
            String provider,
            Path controlledDirectory,
            Path pluginJar,
            PluginCompatibilitySummary compatibility,
            Map<String, ?> rawMetadata) {
        Objects.requireNonNull(controlledDirectory, "controlledDirectory must not be null");
        Objects.requireNonNull(pluginJar, "pluginJar must not be null");
        return new PluginDescriptorSummary(pluginId, name, version, provider,
                summarizePath(controlledDirectory, pluginJar), compatibility, summarizeMetadata(rawMetadata));
    }

    static String summarizePath(Path controlledDirectory, Path pluginJar) {
        Path normalizedDirectory = controlledDirectory.toAbsolutePath().normalize();
        Path normalizedJar = pluginJar.toAbsolutePath().normalize();
        if (normalizedJar.startsWith(normalizedDirectory)) {
            String relative = normalizedDirectory.relativize(normalizedJar).toString();
            return relative.replace('\\', '/');
        }
        Path fileName = normalizedJar.getFileName();
        return fileName == null ? "<unknown>" : fileName.toString();
    }

    private static Map<String, String> summarizeMetadata(Map<String, ?> rawMetadata) {
        Objects.requireNonNull(rawMetadata, "rawMetadata must not be null");
        if (rawMetadata.isEmpty()) {
            return Map.of();
        }
        String metadataKeys = rawMetadata.keySet().stream()
                .map(String::valueOf)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
        return Map.of("metadataKeys", metadataKeys);
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
