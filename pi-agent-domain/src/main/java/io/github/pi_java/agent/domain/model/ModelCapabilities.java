package io.github.pi_java.agent.domain.model;

import java.util.Objects;

public record ModelCapabilities(
        boolean supportsStreamingText,
        boolean supportsToolCallIntents,
        UsageReporting usageReporting,
        int contextWindowTokens,
        int maxOutputTokens,
        boolean supportsProviderExtraParameters
) {
    public ModelCapabilities {
        Objects.requireNonNull(usageReporting, "usageReporting must not be null");
        DomainModelValidation.requirePositive(contextWindowTokens, "contextWindowTokens");
        DomainModelValidation.requirePositive(maxOutputTokens, "maxOutputTokens");
    }

    public enum UsageReporting {
        AVAILABLE,
        OPTIONAL,
        UNAVAILABLE,
        UNKNOWN
    }
}
