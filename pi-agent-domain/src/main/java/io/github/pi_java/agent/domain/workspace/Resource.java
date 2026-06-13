package io.github.pi_java.agent.domain.workspace;

import java.util.Map;

/**
 * Logical resource addressable from a workspace without exposing host details.
 */
public record Resource(String resourceId, String kind, String uri, Map<String, String> metadata) {
    public Resource {
        metadata = Map.copyOf(metadata);
    }
}
