package io.github.pi_java.agent.domain.tool;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ToolSchema(
        String dialect,
        Map<String, Object> document,
        Set<String> sensitiveFields,
        int payloadLimitBytes
) {
    public ToolSchema {
        dialect = ToolValidation.requireNonBlank(dialect, "dialect");
        document = Map.copyOf(Objects.requireNonNull(document, "document must not be null"));
        sensitiveFields = Set.copyOf(Objects.requireNonNull(sensitiveFields, "sensitiveFields must not be null"));
        if (payloadLimitBytes <= 0) {
            throw new IllegalArgumentException("payloadLimitBytes must be greater than zero");
        }
    }
}
