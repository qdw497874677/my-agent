package io.github.pi_java.agent.domain.workspace;

import java.time.Instant;
import java.util.Map;

/**
 * Contract-first snapshot metadata for restore/replay decisions.
 */
public record WorkspaceSnapshot(
        String snapshotId,
        String workspaceId,
        String fingerprint,
        Map<String, String> driftMetadata,
        boolean replaySafe,
        Instant createdAt) {
    public WorkspaceSnapshot {
        driftMetadata = Map.copyOf(driftMetadata);
    }
}
