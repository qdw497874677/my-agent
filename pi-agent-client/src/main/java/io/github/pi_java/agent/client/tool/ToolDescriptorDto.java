package io.github.pi_java.agent.client.tool;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ToolDescriptorDto(
        String id,
        String name,
        String description,
        SchemaDto inputSchema,
        Optional<SchemaDto> outputSchema,
        ProvenanceDto provenance,
        String version,
        Set<String> scopes,
        String riskLevel,
        String sideEffect,
        Duration defaultTimeout,
        Map<String, Object> metadata
) {
    public ToolDescriptorDto {
        scopes = Set.copyOf(scopes);
        outputSchema = outputSchema.map(schema -> schema);
        metadata = Map.copyOf(metadata);
    }

    public record SchemaDto(
            String dialect,
            Map<String, Object> document,
            Set<String> sensitiveFields,
            int payloadLimitBytes
    ) {
        public SchemaDto {
            document = Map.copyOf(document);
            sensitiveFields = Set.copyOf(sensitiveFields);
        }
    }

    public record ProvenanceDto(
            String sourceKind,
            String sourceId,
            String bindingRef,
            Map<String, String> metadata
    ) {
        public ProvenanceDto {
            metadata = Map.copyOf(metadata);
        }
    }
}
