package io.github.pi_java.agent.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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

    // Phase 16: explicitly lock the conversation read-model boundary so the new
    // ConversationQueryService / DefaultConversationQueryService / assembler,
    // the ConversationRunView orchestration type, and the ownership-aware
    // persistence ports can never grow dependencies on Spring, Vaadin, JDBC,
    // SQLite, Infrastructure, adapter-web, or provider SDKs. This is strictly
    // additive and does not weaken the broader app dependency rule above.
    @Test
    void conversation_read_model_and_persistence_ports_must_not_leak_outer_layers() {
        JavaClasses applicationClasses = new ClassFileImporter()
                .importPackages("io.github.pi_java.agent");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.app.port.persistence..")
                .or().haveFullyQualifiedName("io.github.pi_java.agent.app.usecase.ConversationQueryService")
                .or().haveFullyQualifiedName("io.github.pi_java.agent.app.usecase.DefaultConversationQueryService")
                .or().haveFullyQualifiedName("io.github.pi_java.agent.app.usecase.ConversationTranscriptAssembler")
                .or().haveFullyQualifiedName("io.github.pi_java.agent.app.usecase.ConversationRunView")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.vaadin..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "org.sqlite..",
                        "io.github.pi_java.agent.infrastructure..",
                        "io.github.pi_java.agent.adapter..")
                .check(applicationClasses);
    }
}
