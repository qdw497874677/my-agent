package io.github.pi_java.agent.client.tool;

import java.util.List;

public record ToolCatalogResponse(List<ToolDescriptorDto> tools) {
    public ToolCatalogResponse {
        tools = List.copyOf(tools);
    }
}
