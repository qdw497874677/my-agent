package io.github.pi_java.agent.client.conversation;

import java.time.Instant;
import java.util.Map;

/**
 * Typed transcript message contract for the Phase 16 conversation read model.
 *
 * <p>Carries the fields required by decision D-08: role, text, session/run
 * refs, status, timestamps, ordering identity, metadata, and
 * redaction/visibility. This replaces raw {@code Map<String,Object>} history
 * entries (D-13) with a first-class client schema.
 *
 * <p>The {@code metadata} field is defensively normalized: a {@code null}
 * argument becomes an empty immutable map, and a non-null argument is copied
 * into an immutable map so callers cannot mutate the record's state after
 * construction.
 *
 * <p>This record lives in {@code pi-agent-client} and intentionally has no
 * dependency on Domain, Spring, Jackson, Jakarta, Vaadin, or Infrastructure
 * types.
 */
public record ConversationMessageDto(
        String messageId,
        String sessionId,
        String runId,
        String stepId,
        ConversationMessageRole role,
        String text,
        ConversationMessageStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long firstSequence,
        Long lastSequence,
        Map<String, Object> metadata,
        boolean visible,
        boolean redacted) {

    public ConversationMessageDto {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
