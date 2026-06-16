package io.github.pi_java.agent.infrastructure.mcp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class McpInfrastructureArchitectureTest {

    @Test
    void core_client_extension_and_starter_modules_must_not_depend_on_mcp_sdk_or_spring_ai_mcp() {
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
                        "io.modelcontextprotocol..",
                        "org.springframework.ai.mcp..",
                        "org.springframework.ai.tool.execution..",
                        "org.springframework.ai.tool.method..")
                .check(classes);
    }

    @Test
    void mcp_infrastructure_must_not_depend_on_adapter_web_vaadin_pf4j_or_plugin_packages() {
        JavaClasses classes = new ClassFileImporter().importPackages("io.github.pi_java.agent.infrastructure.mcp");

        noClasses().that().resideInAPackage("io.github.pi_java.agent.infrastructure.mcp..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.pi_java.agent.adapter..",
                        "com.vaadin..",
                        "org.pf4j..",
                        "io.github.pi_java.agent.infrastructure.plugin..",
                        "io.github.pi_java.agent.plugin..")
                .check(classes);
    }
}
