package io.github.pi_java.agent.client.event;

import java.util.List;

public record RedactionDto(
        boolean redacted,
        List<String> redactedFields,
        String policy
) {
}
