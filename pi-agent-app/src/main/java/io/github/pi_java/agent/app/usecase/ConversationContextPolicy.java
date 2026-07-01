package io.github.pi_java.agent.app.usecase;

/**
 * Configurable Phase 19 policy for selected-session multi-turn model context.
 *
 * <p>The policy intentionally lives in the App usecase layer: it owns product
 * context rules while remaining plain Java and independent of Vaadin,
 * provider SDKs, Spring AI, persistence, and infrastructure implementations.
 */
public record ConversationContextPolicy(
        int maxRecentMessages,
        int maxTotalCharacters,
        int transcriptLimit) {

    private static final int DEFAULT_MAX_RECENT_MESSAGES = 12;
    private static final int DEFAULT_MAX_TOTAL_CHARACTERS = 12_000;
    private static final int DEFAULT_TRANSCRIPT_LIMIT = 48;

    public ConversationContextPolicy {
        if (maxRecentMessages <= 0) {
            throw new IllegalArgumentException("maxRecentMessages must be positive");
        }
        if (maxTotalCharacters <= 0) {
            throw new IllegalArgumentException("maxTotalCharacters must be positive");
        }
        if (transcriptLimit <= 0) {
            throw new IllegalArgumentException("transcriptLimit must be positive");
        }
        if (transcriptLimit < maxRecentMessages) {
            throw new IllegalArgumentException("transcriptLimit must be greater than or equal to maxRecentMessages");
        }
    }

    public static ConversationContextPolicy defaults() {
        return new ConversationContextPolicy(
                DEFAULT_MAX_RECENT_MESSAGES,
                DEFAULT_MAX_TOTAL_CHARACTERS,
                DEFAULT_TRANSCRIPT_LIMIT);
    }
}
