package io.github.pi_java.agent.client.admin;

import java.util.List;

public record ExtensionGovernanceResponse(List<ExtensionSourceDto> sources) {
    public ExtensionGovernanceResponse {
        sources = List.copyOf(sources == null ? List.of() : sources);
    }
}
