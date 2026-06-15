package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;
import io.github.pi_java.agent.spring.annotation.PiTool;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

public final class AnnotatedToolExtensionSourceFactory {

    public Optional<ExtensionSource> create(ConfigurableListableBeanFactory beanFactory) {
        List<ToolExtensionCapability> capabilities = new ArrayList<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType == null) {
                continue;
            }
            Map<Method, PiTool> methods = MethodIntrospector.selectMethods(beanType,
                    (MethodIntrospector.MetadataLookup<PiTool>) method ->
                            AnnotatedElementUtils.findMergedAnnotation(method, PiTool.class));
            if (methods.isEmpty()) {
                continue;
            }
            Object bean = beanFactory.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            methods.forEach((method, annotation) -> capabilities.add(capability(beanName, bean, targetClass, method, annotation)));
        }
        if (capabilities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnnotatedExtensionSource("spring-annotations", capabilities.stream()
                .map(ExtensionCapability.class::cast).toList()));
    }

    private static ToolExtensionCapability capability(String beanName, Object bean, Class<?> targetClass, Method method,
                                                      PiTool annotation) {
        Method invocableMethod = AopUtils.selectInvocableMethod(method, targetClass);
        validateSignature(invocableMethod);
        String bindingRef = beanName + "#" + invocableMethod.getName();
        Map<String, Object> metadata = metadata(annotation.metadata());
        metadata.put("sourceKind", "SPRING_BEAN");
        metadata.put("bindingRef", bindingRef);
        ToolProvenance provenance = new ToolProvenance(ToolProvenance.SourceKind.SPRING_BEAN,
                "spring-annotations", bindingRef, Map.of("extension.sourceKind", "SPRING_BEAN"));
        ToolDescriptor descriptor = new ToolDescriptor(annotation.id(),
                annotation.name().isBlank() ? annotation.id() : annotation.name(), annotation.description(),
                inputSchema(annotation.inputSchema()), Optional.empty(), provenance, annotation.version(),
                Set.of(annotation.scopes()), annotation.risk(), annotation.sideEffect(),
                Duration.ofMillis(annotation.timeoutMs()), Map.copyOf(metadata));
        return new ToolExtensionCapability(annotation.id(), descriptor, (request, cancellationToken) -> {
            Object output = invoke(bean, invocableMethod, request);
            if (output instanceof ToolExecutionResult result) {
                return result;
            }
            Map<String, Object> outputMap = output instanceof Map<?, ?> map ? copyStringKeyed(map) : Map.of("value", output);
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "Annotated Spring tool completed", Optional.empty(), Map.copyOf(request.arguments()), outputMap,
                    Set.of(), Optional.empty(), Duration.ZERO, Optional.of(outputMap));
        }, Map.copyOf(metadata));
    }

    private static void validateSignature(Method method) {
        int parameterCount = method.getParameterCount();
        if (parameterCount > 1 || parameterCount == 1 && method.getParameterTypes()[0] != ToolExecutionRequest.class) {
            throw new IllegalStateException("@PiTool method must accept no arguments or ToolExecutionRequest: " + method);
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE) {
            throw new IllegalStateException("@PiTool method must return ToolExecutionResult, Map, or a value: " + method);
        }
    }

    private static Object invoke(Object bean, Method method, ToolExecutionRequest request) {
        ReflectionUtils.makeAccessible(method);
        return ReflectionUtils.invokeMethod(method, bean, method.getParameterCount() == 1 ? new Object[]{request} : new Object[]{});
    }

    private static ToolSchema inputSchema(String schema) {
        return new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of("type", "object"), Set.of(), 16_384);
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

    private static Map<String, Object> copyStringKeyed(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return Map.copyOf(result);
    }

    record AnnotatedExtensionSource(String sourceId, List<ExtensionCapability> capabilities) implements ExtensionSource {
        @Override
        public ExtensionMetadata metadata() {
            return new ExtensionMetadata(sourceId, "Spring annotation extensions", "1.0.0", "spring",
                    ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                    ExtensionLifecycleState.STARTED, ExtensionHealth.up("Annotation scan complete"), true,
                    Map.of("sourceKind", "SPRING_BEAN", "registration", "annotation"));
        }
    }
}
