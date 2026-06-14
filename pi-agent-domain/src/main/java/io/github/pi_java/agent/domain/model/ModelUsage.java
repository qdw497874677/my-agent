package io.github.pi_java.agent.domain.model;

public record ModelUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
    public ModelUsage {
        requireNonNegative(inputTokens, "inputTokens");
        requireNonNegative(outputTokens, "outputTokens");
        requireNonNegative(totalTokens, "totalTokens");
    }

    private static void requireNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
