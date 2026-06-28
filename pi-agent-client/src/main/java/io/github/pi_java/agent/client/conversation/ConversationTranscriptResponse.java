package io.github.pi_java.agent.client.conversation;

import java.util.List;
import java.util.Map;

/**
 * Typed transcript response contract for the Phase 16 conversation read model
 * (requirement SESS-04, decisions D-05, D-13, D-16).
 *
 * <p>The {@code messages} component is a typed {@code List<ConversationMessageDto>}
 * rather than raw {@code List<Map<String,Object>>} history entries. This is
 * the canonical chat transcript boundary for downstream App, REST, Console,
 * and future clients.
 *
 * <p>Both the {@code messages} list and the {@code metadata} map are
 * defensively normalized: a {@code null} argument becomes an empty immutable
 * collection/map, and a non-null argument is copied into an immutable view so
 * callers cannot mutate the record's state after construction.
 *
 * <p>This record lives in {@code pi-agent-client} and intentionally has no
 * dependency on Domain, Spring, Jackson, Jakarta, Vaadin, or Infrastructure
 * types.
 */
public record ConversationTranscriptResponse(
        String sessionId,
        List<ConversationMessageDto> messages,
        String activeRunId,
        String activeRunStatus,
        String nextCursor,
        boolean hasMore,
        Map<String, Object> metadata) {

    public ConversationTranscriptResponse {
        messages = messages == null ? List.of() : List.copyOf(messages);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
