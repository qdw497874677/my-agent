package io.github.pi_java.agent.extension.api;

import java.util.List;

public interface ExtensionSource {

    ExtensionMetadata metadata();

    List<ExtensionCapability> capabilities();
}
