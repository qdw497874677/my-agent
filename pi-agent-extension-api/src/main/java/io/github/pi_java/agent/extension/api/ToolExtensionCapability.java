package io.github.pi_java.agent.extension.api;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.Map;
import java.util.Objects;

public record ToolExtensionCapability(
        String capabilityId,
        ToolDescriptor descriptor,
        ToolExecutorBinding binding,
        Map<String, Object> redactedMetadata
) implements ExtensionCapability {
    public ToolExtensionCapability {
        capabilityId = ExtensionStrings.requireNonBlank(capabilityId, "capabilityId");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(binding, "binding must not be null");
        redactedMetadata = Map.copyOf(Objects.requireNonNull(redactedMetadata, "redactedMetadata must not be null"));
    }

    @Override
    public Type type() {
        return Type.TOOL;
    }
}
