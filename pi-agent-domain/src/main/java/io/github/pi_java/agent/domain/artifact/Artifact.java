package io.github.pi_java.agent.domain.artifact;

import java.util.Map;

/** Structured work product produced or consumed independently from messages. */
public record Artifact(String artifactId, String kind, String title, String summary, Map<String, String> metadata) {
    public Artifact {
        metadata = Map.copyOf(metadata);
    }
}
