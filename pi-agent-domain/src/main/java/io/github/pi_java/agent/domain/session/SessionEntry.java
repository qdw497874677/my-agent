package io.github.pi_java.agent.domain.session;

import java.time.Instant;

/** Single append-only tree entry. parentEntryId is null for the root entry. */
public record SessionEntry(
        String entryId,
        String parentEntryId,
        Instant timestamp,
        SessionEntryPayload payload) {}
