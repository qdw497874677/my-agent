package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.spring.annotation.PiEventListener;
import io.github.pi_java.agent.spring.annotation.PiTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedSpringExtensionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PiAgentExtensionAutoConfiguration.class));

    @Test
    void piToolIsRuntimeMethodAnnotationWithGovernanceMetadata() {
        Retention retention = PiTool.class.getAnnotation(Retention.class);
        Target target = PiTool.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(target.value()).contains(ElementType.METHOD);

        PiTool annotation = AnnotatedFixture.class.getDeclaredMethods()[0].getAnnotation(PiTool.class);
        assertThat(annotation.id()).isEqualTo("annotated.echo");
        assertThat(annotation.name()).isEqualTo("Annotated Echo");
        assertThat(annotation.description()).isEqualTo("Echoes a test response");
        assertThat(annotation.version()).isEqualTo("1.2.3");
        assertThat(annotation.scopes()).containsExactly("test:read");
        assertThat(annotation.risk()).isEqualTo(ToolRiskLevel.LOW);
        assertThat(annotation.sideEffect()).isEqualTo(ToolSideEffect.READ_ONLY);
        assertThat(annotation.timeoutMs()).isEqualTo(2500L);
        assertThat(annotation.inputSchema()).contains("object");
        assertThat(annotation.metadata()).containsExactly("team=platform");
    }

    @Test
    void piEventListenerIsRuntimeMethodAnnotationWithLimitedMetadata() {
        Retention retention = PiEventListener.class.getAnnotation(Retention.class);
        Target target = PiEventListener.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(target.value()).contains(ElementType.METHOD);

        PiEventListener annotation = EventFixture.class.getDeclaredMethods()[0].getAnnotation(PiEventListener.class);
        assertThat(annotation.id()).isEqualTo("annotated.listener");
        assertThat(annotation.eventTypes()).containsExactly("tool.lifecycle");
        assertThat(annotation.order()).isEqualTo(10);
        assertThat(annotation.version()).isEqualTo("2.0.0");
        assertThat(annotation.metadata()).containsExactly("team=platform");
    }

    @Test
    void annotatedBeansContributeExtensionCapabilities() {
        contextRunner.withUserConfiguration(AnnotatedBeanConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(AnnotatedToolExtensionSourceFactory.class);

            ToolRegistry toolRegistry = context.getBean(ToolRegistry.class);
            assertThat(toolRegistry.listTools()).extracting(ToolDescriptor::id).contains("annotated.echo");

            ToolRegistry.ToolResolution resolution = toolRegistry.resolve("annotated.echo").orElseThrow();
            assertThat(resolution.descriptor().provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.SPRING_BEAN);
            assertThat(resolution.descriptor().provenance().sourceId()).isEqualTo("spring-annotations");
            assertThat(resolution.descriptor().provenance().bindingRef()).isEqualTo("annotatedBean#echo");

            ToolExecutionRequest request = new ToolExecutionRequest("call-1", new RunId("run-1"), new StepId("step-1"),
                    "annotated.echo", "1.0.0", Map.of("message", "hello"), Instant.EPOCH);
            ToolExecutionResult result = resolution.executor().execute(request, new CancellationToken());
            assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
            assertThat(result.rawOutput()).hasValueSatisfying(output -> assertThat(output).containsEntry("ok", true));

            assertThat(context.getBean(ExtensionGovernanceCatalog.class).sources())
                    .filteredOn(source -> source.sourceId().equals("spring-annotation-listeners"))
                    .singleElement()
                    .satisfies(source -> assertThat(source.capabilities())
                            .extracting(capability -> capability.type() + ":" + capability.capabilityId())
                            .containsExactly("EVENT_LISTENER:annotated.listener"));
        });
    }

    @Test
    void duplicateAnnotatedAndBeanToolIdsFailByDefault() {
        contextRunner.withUserConfiguration(AnnotatedBeanConfiguration.class, DuplicateAnnotatedBeanConfiguration.class)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasMessageContaining("Duplicate extension capability id 'annotated.echo'"));
    }

    static class AnnotatedFixture {
        @PiTool(
                id = "annotated.echo",
                name = "Annotated Echo",
                description = "Echoes a test response",
                version = "1.2.3",
                scopes = "test:read",
                risk = ToolRiskLevel.LOW,
                sideEffect = ToolSideEffect.READ_ONLY,
                timeoutMs = 2500,
                inputSchema = "{\"type\":\"object\"}",
                metadata = "team=platform")
        Object echo() {
            return java.util.Map.of("ok", true);
        }
    }

    static class EventFixture {
        @PiEventListener(
                id = "annotated.listener",
                eventTypes = "tool.lifecycle",
                order = 10,
                version = "2.0.0",
                metadata = "team=platform")
        void onEvent(Object event) {
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedBeanConfiguration {

        @Bean
        AnnotatedBean annotatedBean() {
            return new AnnotatedBean();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateAnnotatedBeanConfiguration {

        @Bean
        DuplicateAnnotatedBean duplicateAnnotatedBean() {
            return new DuplicateAnnotatedBean();
        }
    }

    static class DuplicateAnnotatedBean {

        @PiTool(id = "annotated.echo", name = "Duplicate Annotated Echo")
        Map<String, Object> duplicateEcho() {
            return Map.of("ok", false);
        }
    }

    static class AnnotatedBean {

        @PiTool(id = "annotated.echo", name = "Annotated Echo", description = "Echoes through annotation")
        Map<String, Object> echo() {
            return Map.of("ok", true);
        }

        @PiEventListener(id = "annotated.listener", eventTypes = "tool.lifecycle")
        void onToolEvent(Object event) {
        }
    }
}
