package io.github.pi_java.agent.infrastructure.observability;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PiTelemetryArchitectureTest {

    @Test
    void core_api_and_starter_modules_must_not_depend_on_observability_implementations() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "io.github.pi_java.agent.domain",
                "io.github.pi_java.agent.app",
                "io.github.pi_java.agent.client",
                "io.github.pi_java.agent.extension.api",
                "io.github.pi_java.agent.spring");

        noClasses().that().resideInAnyPackage(
                        "io.github.pi_java.agent.domain..",
                        "io.github.pi_java.agent.app..",
                        "io.github.pi_java.agent.client..",
                        "io.github.pi_java.agent.extension.api..",
                        "io.github.pi_java.agent.spring..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.micrometer..",
                        "io.opentelemetry..",
                        "ch.qos.logback..")
                .check(classes);
    }
}
