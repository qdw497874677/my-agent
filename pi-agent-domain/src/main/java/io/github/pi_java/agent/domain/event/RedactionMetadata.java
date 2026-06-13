package io.github.pi_java.agent.domain.event;

import java.util.Objects;
import java.util.Set;

public record RedactionMetadata(
        boolean containsSecrets,
        boolean redacted,
        Set<String> redactedFields,
        String policyRef
) {

    public RedactionMetadata {
        redactedFields = Set.copyOf(Objects.requireNonNull(redactedFields, "redactedFields must not be null"));
        policyRef = Objects.requireNonNull(policyRef, "policyRef must not be null");
    }
}
