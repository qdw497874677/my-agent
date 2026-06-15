package io.github.pi_java.agent.infrastructure.extension;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ExtensionRegistrationProperties(
        Set<String> disabledSources,
        Set<String> disabledCapabilities,
        boolean allowDuplicateCapabilityOverrides,
        String platformApiVersion
) {
    public ExtensionRegistrationProperties() {
        this(List.of(), List.of(), false, "1.0.0");
    }

    public ExtensionRegistrationProperties(List<String> disabledSources,
                                           List<String> disabledCapabilities,
                                           boolean allowDuplicateCapabilityOverrides,
                                           String platformApiVersion) {
        this(Set.copyOf(Objects.requireNonNull(disabledSources, "disabledSources must not be null")),
                Set.copyOf(Objects.requireNonNull(disabledCapabilities, "disabledCapabilities must not be null")),
                allowDuplicateCapabilityOverrides,
                platformApiVersion);
    }

    public ExtensionRegistrationProperties {
        disabledSources = Set.copyOf(Objects.requireNonNull(disabledSources, "disabledSources must not be null"));
        disabledCapabilities = Set.copyOf(Objects.requireNonNull(disabledCapabilities,
                "disabledCapabilities must not be null"));
        platformApiVersion = requireNonBlank(platformApiVersion, "platformApiVersion");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
