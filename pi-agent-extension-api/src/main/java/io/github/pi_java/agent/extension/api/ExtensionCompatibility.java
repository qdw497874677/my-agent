package io.github.pi_java.agent.extension.api;

import java.util.Objects;

public record ExtensionCompatibility(
        ExtensionApiVersion minInclusive,
        ExtensionApiVersion maxExclusive
) {
    public ExtensionCompatibility {
        Objects.requireNonNull(minInclusive, "minInclusive must not be null");
        Objects.requireNonNull(maxExclusive, "maxExclusive must not be null");
        if (minInclusive.compareTo(maxExclusive) >= 0) {
            throw new IllegalArgumentException("minInclusive must be lower than maxExclusive");
        }
    }

    public static ExtensionCompatibility supports(String minInclusive, String maxExclusive) {
        return new ExtensionCompatibility(ExtensionApiVersion.parse(minInclusive), ExtensionApiVersion.parse(maxExclusive));
    }

    public boolean supports(ExtensionApiVersion version) {
        Objects.requireNonNull(version, "version must not be null");
        return version.compareTo(minInclusive) >= 0 && version.compareTo(maxExclusive) < 0;
    }
}
