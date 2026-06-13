package io.github.pi_java.agent.domain.session;

import java.util.List;

/** Append-only session tree plus current leaf pointer. */
public record Session(
        String sessionId,
        String tenantId,
        String userId,
        String currentLeafEntryId,
        List<SessionEntry> entries) {
    public Session {
        entries = List.copyOf(entries);
    }
}
