package io.github.pi_java.agent.infrastructure.plugin;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PluginInfrastructureArchitectureTest {

    @Test
    void core_client_extension_starter_mcp_and_provider_modules_must_not_depend_on_pf4j_or_plugin_infrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackages(
                "io.github.pi_java.agent.domain",
                "io.github.pi_java.agent.app",
                "io.github.pi_java.agent.client",
                "io.github.pi_java.agent.extension.api",
                "io.github.pi_java.agent.spring",
                "io.github.pi_java.agent.infrastructure.mcp",
                "io.github.pi_java.agent.infrastructure.model");

        noClasses().that().resideInAnyPackage(
                        "io.github.pi_java.agent.domain..",
                        "io.github.pi_java.agent.app..",
                        "io.github.pi_java.agent.client..",
                        "io.github.pi_java.agent.extension.api..",
                        "io.github.pi_java.agent.spring..",
                        "io.github.pi_java.agent.infrastructure.mcp..",
                        "io.github.pi_java.agent.infrastructure.model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.pf4j..",
                        "io.github.pi_java.agent.infrastructure.plugin..")
                .check(classes);
    }

    @Test
    void plugin_infrastructure_must_not_depend_on_adapter_ui_persistence_mcp_or_model_provider_implementations() {
        JavaClasses classes = new ClassFileImporter().importPackages("io.github.pi_java.agent.infrastructure.plugin");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.infrastructure.plugin..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.pi_java.agent.adapter..",
                        "com.vaadin..",
                        "java.sql..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.flywaydb..",
                        "io.modelcontextprotocol..",
                        "org.springframework.ai..",
                        "io.github.pi_java.agent.infrastructure.mcp..",
                        "io.github.pi_java.agent.infrastructure.model..",
                        "io.github.pi_java.agent.infrastructure.persistence..")
                .check(classes);
    }
}
