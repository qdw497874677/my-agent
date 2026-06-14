package io.github.pi_java.agent.client.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int limit,
        Long afterSequence,
        Long nextAfterSequence,
        boolean hasMore) {
}
