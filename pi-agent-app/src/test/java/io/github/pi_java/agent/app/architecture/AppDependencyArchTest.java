package io.github.pi_java.agent.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class AppDependencyArchTest {

    @Test
    void app_must_only_depend_on_client_domain_and_app_packages() {
        JavaClasses applicationClasses = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent");

        classes().that().resideInAPackage("io.github.pi_java.agent.app..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "org.junit.jupiter.api..",
                        "org.assertj.core..",
                        "com.tngtech.archunit..",
                        "io.github.pi_java.agent.app..",
                        "io.github.pi_java.agent.domain..",
                        "io.github.pi_java.agent.client.."
                )
                .check(applicationClasses);
    }
}
