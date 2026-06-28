package io.github.pi_java.agent.client.conversation;

import java.time.Instant;
import java.util.Map;

/**
 * Typed recent-session summary contract for the Phase 16 conversation read
 * model (requirement SESS-01).
 *
 * <p>Carries title, latest activity, status, safe preview, and optional active
 * run refs for UI/client use. Per Phase 16 decisions D-09..D-12: title is
 * derived from the first user message and remains stable; last-activity
 * reflects the latest visible transcript activity; status is derived from the
 * most recent relevant run/conversation state; preview is the latest visible
 * transcript item after redaction.
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
public record SessionSummaryDto(
        String sessionId,
        String title,
        String status,
        String lastMessagePreview,
        Instant lastActivityAt,
        Instant createdAt,
        String activeRunId,
        String activeRunStatus,
        Map<String, Object> metadata) {

    public SessionSummaryDto {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
