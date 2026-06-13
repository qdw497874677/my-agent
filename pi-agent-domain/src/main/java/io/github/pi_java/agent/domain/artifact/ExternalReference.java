package io.github.pi_java.agent.domain.artifact;

import java.util.Map;

/** Reference to an external resource by URI without embedding the resource. */
public record ExternalReference(String referenceId, String kind, String uri, String title, Map<String, String> metadata) {
    public ExternalReference {
        metadata = Map.copyOf(metadata);
    }
}
