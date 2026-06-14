package io.github.pi_java.agent.domain.model;

public record ProviderModelRef(String providerId, String modelId) {
    public ProviderModelRef {
        providerId = DomainModelValidation.requireNonBlank(providerId, "providerId");
        modelId = DomainModelValidation.requireNonBlank(modelId, "modelId");
        if (providerId.indexOf(':') >= 0 || modelId.indexOf(':') >= 0) {
            throw new IllegalArgumentException("providerId and modelId must not contain ':'");
        }
        if (providerId.chars().anyMatch(Character::isWhitespace)
                || modelId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("providerId and modelId must not contain whitespace");
        }
    }

    public static ProviderModelRef parse(String value) {
        String modelRef = DomainModelValidation.requireNonBlank(value, "modelRef");
        if (!modelRef.equals(modelRef.strip())) {
            throw new IllegalArgumentException("modelRef must not include leading or trailing whitespace");
        }
        int separator = modelRef.indexOf(':');
        if (separator <= 0 || separator != modelRef.lastIndexOf(':') || separator == modelRef.length() - 1) {
            throw new IllegalArgumentException("modelRef must use provider:model syntax");
        }
        return new ProviderModelRef(modelRef.substring(0, separator), modelRef.substring(separator + 1));
    }

    public String canonical() {
        return providerId + ":" + modelId;
    }

    @Override
    public String toString() {
        return canonical();
    }
}
