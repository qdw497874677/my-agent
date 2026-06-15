package io.github.pi_java.agent.app.port.extension;

import java.util.List;

public final class EmptyExtensionGovernanceCatalog implements ExtensionGovernanceCatalog {

    @Override
    public List<ExtensionSourceStatus> sources() {
        return List.of();
    }
}
