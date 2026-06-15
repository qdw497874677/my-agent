package io.github.pi_java.agent.extension.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ExtensionApiArchitectureTest {

    @Test
    void extension_api_must_not_depend_on_frameworks_sdks_adapters_or_infrastructure() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.extension.api");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.extension.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.persistence..",
                        "com.vaadin..",
                        "org.pf4j..",
                        "io.modelcontextprotocol..",
                        "org.springframework.ai..",
                        "com.fasterxml.jackson.annotation..",
                        "java.sql..",
                        "javax.sql..",
                        "dev.langchain4j..",
                        "com.openai..",
                        "software.amazon.awssdk..",
                        "io.github.pi_java.agent.adapter..",
                        "io.github.pi_java.agent.infrastructure.."
                )
                .check(classes);
    }

    @Test
    void domain_and_app_must_not_depend_back_on_extension_api() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.domain", "io.github.pi_java.agent.app");

        noClasses().that().resideInAnyPackage("io.github.pi_java.agent.domain..", "io.github.pi_java.agent.app..")
                .should().dependOnClassesThat().resideInAnyPackage("io.github.pi_java.agent.extension..")
                .check(classes);
    }
}
