package io.github.pi_java.agent.domain.tool;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ToolDescriptor(
        String id,
        String name,
        String description,
        ToolSchema inputSchema,
        Optional<ToolSchema> outputSchema,
        ToolProvenance provenance,
        String version,
        Set<String> scopes,
        ToolRiskLevel riskLevel,
        ToolSideEffect sideEffect,
        Duration defaultTimeout,
        Map<String, Object> metadata
) {
    public ToolDescriptor(
            String id,
            String name,
            String description,
            ToolSchema inputSchema,
            ToolSchema outputSchema,
            ToolProvenance provenance,
            String version,
            Set<String> scopes,
            ToolRiskLevel riskLevel,
            ToolSideEffect sideEffect,
            Duration defaultTimeout,
            Map<String, Object> metadata
    ) {
        this(id, name, description, inputSchema, Optional.ofNullable(outputSchema), provenance, version, scopes,
                riskLevel, sideEffect, defaultTimeout, metadata);
    }

    public ToolDescriptor {
        id = ToolValidation.requireNonBlank(id, "id");
        name = ToolValidation.requireNonBlank(name, "name");
        description = description == null ? "" : description;
        Objects.requireNonNull(inputSchema, "inputSchema must not be null");
        outputSchema = Objects.requireNonNull(outputSchema, "outputSchema must not be null");
        outputSchema.ifPresent(schema -> Objects.requireNonNull(schema, "outputSchema must not contain null"));
        Objects.requireNonNull(provenance, "provenance must not be null");
        version = ToolValidation.requireNonBlank(version, "version");
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes must not be null"));
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(sideEffect, "sideEffect must not be null");
        Objects.requireNonNull(defaultTimeout, "defaultTimeout must not be null");
        if (defaultTimeout.isNegative() || defaultTimeout.isZero()) {
            throw new IllegalArgumentException("defaultTimeout must be greater than zero");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }
}
