package io.github.pi_java.agent.domain.artifact;

import java.util.Map;

/** Binary or opaque runtime attachment tracked separately from chat text. */
public record Attachment(String attachmentId, String kind, String name, String summary, Map<String, String> metadata) {
    public Attachment {
        metadata = Map.copyOf(metadata);
    }
}
