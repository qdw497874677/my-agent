package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Descriptor-first in-memory registry for built-in and deterministic test registrations.
 */
public final class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, ToolResolution> toolsById;

    public InMemoryToolRegistry(List<ToolRegistration> registrations) {
        Objects.requireNonNull(registrations, "registrations must not be null");
        Map<String, ToolResolution> mutable = new LinkedHashMap<>();
        for (ToolRegistration registration : registrations) {
            ToolRegistration nonNull = Objects.requireNonNull(registration, "registration must not be null");
            ToolResolution previous = mutable.putIfAbsent(
                    nonNull.descriptor().id(),
                    new ToolResolution(nonNull.descriptor(), nonNull.executor())
            );
            if (previous != null) {
                throw new IllegalArgumentException("duplicate tool id: " + nonNull.descriptor().id());
            }
        }
        this.toolsById = Map.copyOf(mutable);
    }

    public static InMemoryToolRegistry empty() {
        return new InMemoryToolRegistry(List.of());
    }

    public static InMemoryToolRegistry of(ToolRegistration... registrations) {
        return new InMemoryToolRegistry(List.of(registrations));
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return toolsById.values().stream()
                .map(ToolResolution::descriptor)
                .toList();
    }

    @Override
    public Optional<ToolResolution> resolve(String toolId) {
        if (toolId == null || toolId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(toolsById.get(toolId));
    }

    public InMemoryToolRegistry with(ToolRegistration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        List<ToolRegistration> registrations = new ArrayList<>();
        for (ToolResolution resolution : toolsById.values()) {
            registrations.add(new ToolRegistration(resolution.descriptor(), resolution.executor()));
        }
        registrations.add(registration);
        return new InMemoryToolRegistry(registrations);
    }

    public record ToolRegistration(ToolDescriptor descriptor, ToolExecutorBinding executor) {
        public ToolRegistration {
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            Objects.requireNonNull(executor, "executor must not be null");
        }
    }
}
