package io.github.pi_java.agent.domain.workspace;

/**
 * Logical mount binding a resource into a workspace namespace.
 */
public record Mount(String mountId, String resourceId, String logicalPath, boolean readOnly) {}
