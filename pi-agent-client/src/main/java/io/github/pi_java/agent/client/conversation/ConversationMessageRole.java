package io.github.pi_java.agent.client.conversation;

/**
 * Stable, type-safe role of a transcript message, exposed for JSON clients via
 * a lowercase {@link #wireValue()}.
 *
 * <p>Per Phase 16 decision D-06, transcript roles include {@code user},
 * {@code assistant}, {@code tool}, and {@code error}. Tool/error items may be
 * summarized or redacted but remain first-class typed transcript entries.
 *
 * <p>This enum lives in {@code pi-agent-client} and intentionally has no
 * dependency on Domain, Spring, Jackson, Jakarta, Vaadin, or Infrastructure
 * types.
 */
public enum ConversationMessageRole {

    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    ERROR("error");

    private final String wireValue;

    ConversationMessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return stable lowercase wire value used by JSON/REST clients.
     */
    public String wireValue() {
        return wireValue;
    }
}
