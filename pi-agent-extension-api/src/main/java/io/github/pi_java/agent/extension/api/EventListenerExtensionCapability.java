package io.github.pi_java.agent.extension.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EventListenerExtensionCapability(
        String capabilityId,
        Set<String> eventTypes,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public EventListenerExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        eventTypes = Set.copyOf(Objects.requireNonNull(eventTypes, "eventTypes must not be null"));
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.EVENT_LISTENER;
    }
}
