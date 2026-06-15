package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.extension.api.ExtensionSource;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

public final class SpringExtensionSourceFactory {

    public List<ExtensionSource> orderedSources(ObjectProvider<ExtensionSource> extensionSources) {
        return extensionSources.orderedStream().toList();
    }
}
