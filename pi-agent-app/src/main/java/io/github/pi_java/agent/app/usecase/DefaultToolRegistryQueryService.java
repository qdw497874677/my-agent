package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
import io.github.pi_java.agent.client.tool.ToolDescriptorDto;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolSchema;

import java.util.Objects;

public final class DefaultToolRegistryQueryService implements ToolRegistryQueryService {
    private final ToolRegistry registry;

    public DefaultToolRegistryQueryService(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    @Override
    public ToolCatalogResponse listTools(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new ToolCatalogResponse(registry.listTools().stream()
                .map(DefaultToolRegistryQueryService::toDto)
                .toList());
    }

    private static ToolDescriptorDto toDto(ToolDescriptor descriptor) {
        return new ToolDescriptorDto(
                descriptor.id(),
                descriptor.name(),
                descriptor.description(),
                toDto(descriptor.inputSchema()),
                descriptor.outputSchema().map(DefaultToolRegistryQueryService::toDto),
                toDto(descriptor.provenance()),
                descriptor.version(),
                descriptor.scopes(),
                descriptor.riskLevel().name(),
                descriptor.sideEffect().name(),
                descriptor.defaultTimeout(),
                descriptor.metadata());
    }

    private static ToolDescriptorDto.SchemaDto toDto(ToolSchema schema) {
        return new ToolDescriptorDto.SchemaDto(
                schema.dialect(),
                schema.document(),
                schema.sensitiveFields(),
                schema.payloadLimitBytes());
    }

    private static ToolDescriptorDto.ProvenanceDto toDto(ToolProvenance provenance) {
        return new ToolDescriptorDto.ProvenanceDto(
                provenance.sourceKind().name(),
                provenance.sourceId(),
                provenance.bindingRef(),
                provenance.metadata());
    }
}
