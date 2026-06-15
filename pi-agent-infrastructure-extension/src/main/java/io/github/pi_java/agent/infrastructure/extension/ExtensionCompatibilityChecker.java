package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;

import java.util.Objects;

public final class ExtensionCompatibilityChecker {

    private final ExtensionApiVersion platformVersion;

    public ExtensionCompatibilityChecker(ExtensionApiVersion platformVersion) {
        this.platformVersion = Objects.requireNonNull(platformVersion, "platformVersion must not be null");
    }

    public boolean supports(ExtensionCompatibility compatibility) {
        return Objects.requireNonNull(compatibility, "compatibility must not be null").supports(platformVersion);
    }

    public boolean supports(String rangeOrExactVersion) {
        return parse(rangeOrExactVersion).contains(platformVersion);
    }

    static CompatibilityRange parse(String value) {
        String normalized = requireNonBlank(value, "compatibility").trim();
        if (normalized.startsWith("[") || normalized.startsWith("(")) {
            if (!(normalized.endsWith("]") || normalized.endsWith(")"))) {
                throw new IllegalArgumentException("compatibility range must end with ] or )");
            }
            boolean minInclusive = normalized.startsWith("[");
            boolean maxInclusive = normalized.endsWith("]");
            String inner = normalized.substring(1, normalized.length() - 1);
            String[] parts = inner.split(",", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("compatibility range must contain min,max");
            }
            return new CompatibilityRange(ExtensionApiVersion.parse(parts[0].trim()), minInclusive,
                    ExtensionApiVersion.parse(parts[1].trim()), maxInclusive);
        }
        ExtensionApiVersion exact = ExtensionApiVersion.parse(normalized);
        return new CompatibilityRange(exact, true, exact, true);
    }

    record CompatibilityRange(
            ExtensionApiVersion min,
            boolean minInclusive,
            ExtensionApiVersion max,
            boolean maxInclusive
    ) {
        CompatibilityRange {
            Objects.requireNonNull(min, "min must not be null");
            Objects.requireNonNull(max, "max must not be null");
            if (min.compareTo(max) > 0) {
                throw new IllegalArgumentException("min must be lower than or equal to max");
            }
        }

        boolean contains(ExtensionApiVersion version) {
            int minComparison = version.compareTo(min);
            int maxComparison = version.compareTo(max);
            return (minInclusive ? minComparison >= 0 : minComparison > 0)
                    && (maxInclusive ? maxComparison <= 0 : maxComparison < 0);
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
