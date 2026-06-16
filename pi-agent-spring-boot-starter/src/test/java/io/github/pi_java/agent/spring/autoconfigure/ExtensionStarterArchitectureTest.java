package io.github.pi_java.agent.spring.autoconfigure;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ExtensionStarterArchitectureTest {

    @Test
    void starter_must_not_depend_on_adapters_ui_mcp_pf4j_jdbc_or_provider_sdks() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.spring");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.spring..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.pi_java.agent.adapter..",
                        "com.vaadin..",
                        "org.pf4j..",
                        "io.modelcontextprotocol..",
                        "org.springframework.ai..",
                        "java.sql..",
                        "javax.sql..",
                        "dev.langchain4j..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "com.google.cloud.vertexai..",
                        "software.amazon.awssdk..",
                        "software.amazon.bedrockruntime..",
                        "software.amazon.awssdk.services.bedrockruntime.."
                )
                .check(classes);
    }

    @Test
    void domain_and_app_must_not_depend_on_starter_or_infrastructure_extension_modules() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.domain", "io.github.pi_java.agent.app");

        noClasses().that().resideInAnyPackage("io.github.pi_java.agent.domain..", "io.github.pi_java.agent.app..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.pi_java.agent.spring..",
                        "io.github.pi_java.agent.infrastructure.extension..",
                        "io.github.pi_java.agent.extension.api.."
                )
                .check(classes);
    }

    @Test
    void infrastructure_extension_must_not_depend_back_on_spring_starter_or_adapters() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.infrastructure.extension");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.infrastructure.extension..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.pi_java.agent.spring..",
                        "io.github.pi_java.agent.adapter..",
                        "com.vaadin..",
                        "org.pf4j..",
                        "io.modelcontextprotocol..",
                        "org.springframework.ai.."
                )
                .check(classes);
    }
}
