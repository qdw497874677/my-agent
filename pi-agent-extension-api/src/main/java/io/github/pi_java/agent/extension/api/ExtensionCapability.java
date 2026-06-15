package io.github.pi_java.agent.extension.api;

import java.util.Map;

public interface ExtensionCapability {

    String capabilityId();

    Type type();

    Map<String, Object> redactedMetadata();

    enum Type {
        TOOL,
        MODEL_PROVIDER,
        POLICY,
        EVENT_LISTENER,
        WORKSPACE_PROVIDER,
        MEMORY_PROVIDER
    }
}
