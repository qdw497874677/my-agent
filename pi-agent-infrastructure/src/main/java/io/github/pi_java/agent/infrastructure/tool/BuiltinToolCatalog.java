package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.List;
import java.util.Objects;

/**
 * Minimum built-in descriptor/binding catalog for governed E2E coverage.
 */
public final class BuiltinToolCatalog {
    private final List<InMemoryToolRegistry.ToolRegistration> registrations;

    public BuiltinToolCatalog(
            ReadOnlyInfoTool readOnlyInfoTool,
            WorkspaceResourceWriteTool workspaceResourceWriteTool,
            WorkspaceCommandTool workspaceCommandTool
    ) {
        this.registrations = List.of(
                Objects.requireNonNull(readOnlyInfoTool, "readOnlyInfoTool must not be null").registration(),
                Objects.requireNonNull(workspaceResourceWriteTool, "workspaceResourceWriteTool must not be null").registration(),
                Objects.requireNonNull(workspaceCommandTool, "workspaceCommandTool must not be null").registration()
        );
    }

    public List<InMemoryToolRegistry.ToolRegistration> registrations() {
        return registrations;
    }

    public List<ToolDescriptor> descriptors() {
        return registrations.stream().map(InMemoryToolRegistry.ToolRegistration::descriptor).toList();
    }

    public InMemoryToolRegistry registry() {
        return new InMemoryToolRegistry(registrations);
    }
}
