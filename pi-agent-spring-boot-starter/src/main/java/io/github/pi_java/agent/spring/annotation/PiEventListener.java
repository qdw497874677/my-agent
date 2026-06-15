package io.github.pi_java.agent.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a lightweight Spring bean method as a Pi event listener extension capability.
 * <p>
 * The annotation contributes listener metadata for governance and future listener dispatch bridges only. It does not
 * replace durable {@code EventSink} persistence or fanout semantics. Complex extension capabilities such as model
 * providers, policy engines, workspace/resource providers, memory providers, or advanced multi-capability extensions
 * must be registered as explicit {@code ExtensionSource} Spring beans.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PiEventListener {

    String id();

    String[] eventTypes();

    int order() default 0;

    String version() default "1.0.0";

    String[] metadata() default {};
}
