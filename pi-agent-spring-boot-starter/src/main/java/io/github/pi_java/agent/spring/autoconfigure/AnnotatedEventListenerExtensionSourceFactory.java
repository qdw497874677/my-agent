package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.extension.api.EventListenerExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.spring.annotation.PiEventListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

public final class AnnotatedEventListenerExtensionSourceFactory {

    public Optional<ExtensionSource> create(ConfigurableListableBeanFactory beanFactory) {
        List<EventListenerExtensionCapability> capabilities = new ArrayList<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType == null) {
                continue;
            }
            Map<Method, PiEventListener> methods = MethodIntrospector.selectMethods(beanType,
                    (MethodIntrospector.MetadataLookup<PiEventListener>) method ->
                            AnnotatedElementUtils.findMergedAnnotation(method, PiEventListener.class));
            methods.forEach((method, annotation) -> capabilities.add(capability(beanName, method, annotation)));
        }
        if (capabilities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnnotatedToolExtensionSourceFactory.AnnotatedExtensionSource(
                "spring-annotation-listeners", capabilities.stream().map(ExtensionCapability.class::cast).toList()));
    }

    private static EventListenerExtensionCapability capability(String beanName, Method method, PiEventListener annotation) {
        Map<String, Object> metadata = metadata(annotation.metadata());
        metadata.put("sourceKind", "SPRING_BEAN");
        metadata.put("bindingRef", beanName + "#" + method.getName());
        metadata.put("order", annotation.order());
        metadata.put("version", annotation.version());
        return new EventListenerExtensionCapability(annotation.id(), Set.of(annotation.eventTypes()), Map.copyOf(metadata));
    }

    private static Map<String, Object> metadata(String[] metadata) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String entry : metadata) {
            int split = entry.indexOf('=');
            if (split > 0) {
                result.put(entry.substring(0, split), entry.substring(split + 1));
            }
        }
        return result;
    }
}
