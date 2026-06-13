package io.github.pi_java.agent.domain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainDependencyArchTest {

    @Test
    void domain_must_not_depend_on_outer_frameworks_or_sdks() {
        JavaClasses domainClasses = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent.domain");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.domain..")
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
                        "software.amazon.awssdk.."
                )
                .check(domainClasses);
    }
}
