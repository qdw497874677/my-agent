package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ToolRegistry {

    List<ToolDescriptor> listTools();

    Optional<ToolResolution> resolve(String toolId);

    record ToolResolution(ToolDescriptor descriptor, ToolExecutorBinding executor) {
        public ToolResolution {
            Objects.requireNonNull(descriptor, "descriptor must not be null");
            Objects.requireNonNull(executor, "executor must not be null");
        }
    }
}
