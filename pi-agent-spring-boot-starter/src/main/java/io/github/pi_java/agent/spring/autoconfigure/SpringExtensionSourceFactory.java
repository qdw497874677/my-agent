package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.extension.api.ExtensionSource;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public final class SpringExtensionSourceFactory {

    public List<ExtensionSource> orderedSources(ObjectProvider<ExtensionSource> extensionSources) {
        return extensionSources.orderedStream().toList();
    }

    public List<ExtensionSource> orderedSources(
            ObjectProvider<ExtensionSource> extensionSources,
            ConfigurableListableBeanFactory beanFactory,
            AnnotatedToolExtensionSourceFactory annotatedToolFactory,
            AnnotatedEventListenerExtensionSourceFactory annotatedEventListenerFactory) {
        return Stream.concat(extensionSources.orderedStream(), Stream.of(
                        annotatedToolFactory.create(beanFactory),
                        annotatedEventListenerFactory.create(beanFactory)).flatMap(java.util.Optional::stream))
                .toList();
    }
}
