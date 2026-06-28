package io.github.pi_java.agent.client.conversation;

/**
 * Stable, type-safe lifecycle status of a transcript message, exposed for JSON
 * clients via a lowercase {@link #wireValue()}.
 *
 * <p>Per Phase 16 decision D-07, conversation messages carry explicit status
 * fields (pending/completed/failed/cancelled/partial) so finish, error, and
 * cancel transitions are modeled as state changes rather than blank assistant
 * messages.
 *
 * <p>This enum lives in {@code pi-agent-client} and intentionally has no
 * dependency on Domain, Spring, Jackson, Jakarta, Vaadin, or Infrastructure
 * types.
 */
public enum ConversationMessageStatus {

    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    PARTIAL("partial");

    private final String wireValue;

    ConversationMessageStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return stable lowercase wire value used by JSON/REST clients.
     */
    public String wireValue() {
        return wireValue;
    }
}
