package io.github.pi_java.agent.app.usecase;

/**
 * Observable metadata emitted by {@link ConversationContextAssembler} when it
 * reduces a selected-session transcript into bounded model-visible context.
 */
public record ConversationContextMetadata(
        int includedCount,
        int droppedCount,
        int excludedCount,
        int maxTotalCharacters,
        int resultingCharacters,
        boolean truncated) {

    public ConversationContextMetadata {
        if (includedCount < 0) {
            throw new IllegalArgumentException("includedCount must not be negative");
        }
        if (droppedCount < 0) {
            throw new IllegalArgumentException("droppedCount must not be negative");
        }
        if (excludedCount < 0) {
            throw new IllegalArgumentException("excludedCount must not be negative");
        }
        if (maxTotalCharacters <= 0) {
            throw new IllegalArgumentException("maxTotalCharacters must be positive");
        }
        if (resultingCharacters < 0) {
            throw new IllegalArgumentException("resultingCharacters must not be negative");
        }
    }
}
