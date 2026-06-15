package io.github.pi_java.agent.client.agent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AgentCatalogItemDto(
        String id,
        String name,
        String description,
        Set<String> supportedInputModes,
        Set<String> capabilities,
        ModelRefDto modelRef,
        Set<String> allowedToolIds,
        Set<String> allowedToolScopes,
        Set<String> riskLabels,
        Set<String> sideEffectLabels,
        List<EntryActionDto> entryActions,
        Duration defaultTimeout,
        Map<String, Object> metadata
) {
    public AgentCatalogItemDto {
        supportedInputModes = Set.copyOf(supportedInputModes);
        capabilities = Set.copyOf(capabilities);
        allowedToolIds = Set.copyOf(allowedToolIds);
        allowedToolScopes = Set.copyOf(allowedToolScopes);
        riskLabels = Set.copyOf(riskLabels);
        sideEffectLabels = Set.copyOf(sideEffectLabels);
        entryActions = List.copyOf(entryActions);
        metadata = Map.copyOf(metadata);
    }

    public record ModelRefDto(
            String provider,
            String model,
            String safeRef
    ) {
    }

    public record EntryActionDto(
            String id,
            String label,
            String actionType,
            String inputMode,
            Map<String, Object> defaults
    ) {
        public EntryActionDto {
            defaults = Map.copyOf(defaults);
        }
    }
}
