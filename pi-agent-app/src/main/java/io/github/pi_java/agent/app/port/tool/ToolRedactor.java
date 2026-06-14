package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@FunctionalInterface
public interface ToolRedactor {

    RedactedToolPayload redact(ToolDescriptor descriptor, Map<String, Object> payload);

    record RedactedToolPayload(Map<String, Object> summary, Set<String> redactedFields, boolean truncated) {
        public RedactedToolPayload {
            summary = Map.copyOf(Objects.requireNonNull(summary, "summary must not be null"));
            redactedFields = Set.copyOf(Objects.requireNonNull(redactedFields, "redactedFields must not be null"));
        }
    }
}
