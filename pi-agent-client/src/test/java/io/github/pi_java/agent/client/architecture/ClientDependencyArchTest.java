package io.github.pi_java.agent.client.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ClientDependencyArchTest {

    @Test
    void client_contracts_do_not_depend_on_runtime_implementations_or_ui_frameworks() {
        JavaClasses clientClasses = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.client");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.client..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.vaadin..",
                        "org.springframework.ai..",
                        "org.sqlite..",
                        "java.sql..",
                        "io.github.pi_java.agent.adapter..",
                        "io.github.pi_java.agent.infrastructure..",
                        "io.github.pi_java.agent.infrastructure.model.openai.."
                )
                .check(clientClasses);
    }
}
