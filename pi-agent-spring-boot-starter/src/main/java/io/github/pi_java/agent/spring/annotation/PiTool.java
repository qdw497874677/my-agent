package io.github.pi_java.agent.spring.annotation;

import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a lightweight Spring bean method as a Pi tool extension.
 * <p>
 * This annotation is intentionally limited to simple method-backed tools. Complex extension capabilities such as model
 * providers, policy engines, workspace/resource providers, memory providers, or advanced multi-capability extensions
 * must be registered as explicit {@code ExtensionSource} Spring beans so their lifecycle, compatibility, health, and
 * governance behavior remains auditable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PiTool {

    String id();

    String name() default "";

    String description() default "";

    String version() default "1.0.0";

    String[] scopes() default {};

    ToolRiskLevel risk() default ToolRiskLevel.LOW;

    ToolSideEffect sideEffect() default ToolSideEffect.READ_ONLY;

    long timeoutMs() default 30_000L;

    String inputSchema() default "";

    String[] metadata() default {};
}
