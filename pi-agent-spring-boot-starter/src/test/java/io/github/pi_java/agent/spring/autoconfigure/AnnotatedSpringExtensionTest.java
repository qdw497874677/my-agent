package io.github.pi_java.agent.spring.autoconfigure;

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
        contextRunner.withUserConfiguration(AnnotatedBeanConfiguration.class).run(context -> assertThat(context)
                .hasSingleBean(AnnotatedToolExtensionSourceFactory.class));
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
