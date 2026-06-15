package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.extension.api.ToolExtensionCapability;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PiAgentExtensionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PiAgentExtensionAutoConfiguration.class));

    @Test
    void createsAutoConfiguredBeansForSpringExtensionSource() {
        contextRunner.withUserConfiguration(SpringToolExtensionConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(DefaultExtensionContributionRegistry.class);
            assertThat(context).hasSingleBean(ToolRegistry.class);
            assertThat(context).hasSingleBean(ExtensionGovernanceCatalog.class);

            ToolRegistry toolRegistry = context.getBean(ToolRegistry.class);
            assertThat(toolRegistry.listTools()).extracting(ToolDescriptor::id).contains("spring.echo");
            ToolDescriptor descriptor = toolRegistry.resolve("spring.echo").orElseThrow().descriptor();
            assertThat(descriptor.provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.SPRING_BEAN);
            assertThat(descriptor.provenance().metadata()).containsEntry("extension.sourceKind", "SPRING_BEAN");
        });
    }

    @Test
    void userProvidedToolRegistryOverridesAutoConfiguredRegistry() {
        ToolRegistry customRegistry = new ToolRegistry() {
            @Override
            public List<ToolDescriptor> listTools() {
                return List.of();
            }

            @Override
            public Optional<ToolResolution> resolve(String toolId) {
                return Optional.empty();
            }
        };

        contextRunner.withBean(ToolRegistry.class, () -> customRegistry)
                .withUserConfiguration(SpringToolExtensionConfiguration.class)
                .run(context -> assertThat(context.getBean(ToolRegistry.class)).isSameAs(customRegistry));
    }

    @Test
    void duplicateCapabilityIdsFailByDefault() {
        contextRunner.withUserConfiguration(DuplicateToolExtensionConfiguration.class).run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasMessageContaining("Duplicate extension capability id"));
    }

    @Test
    void disabledCapabilityRemainsVisibleInGovernanceButAbsentFromToolRegistry() {
        contextRunner.withUserConfiguration(SpringToolExtensionConfiguration.class)
                .withPropertyValues("pi.extensions.disabled-capabilities=spring.echo")
                .run(context -> {
                    ToolRegistry toolRegistry = context.getBean(ToolRegistry.class);
                    assertThat(toolRegistry.listTools()).isEmpty();
                    assertThat(toolRegistry.resolve("spring.echo")).isEmpty();

                    List<ExtensionSourceStatus> sources = context.getBean(ExtensionGovernanceCatalog.class).sources();
                    assertThat(sources).singleElement().satisfies(source -> {
                        assertThat(source.kind()).isEqualTo("SPRING_BEAN");
                        assertThat(source.capabilities()).singleElement().satisfies(capability -> {
                            assertThat(capability.capabilityId()).isEqualTo("spring.echo");
                            assertThat(capability.status()).isEqualTo("DISABLED");
                        });
                    });
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringToolExtensionConfiguration {

        @Bean
        ExtensionSource springExtensionSource() {
            return source("spring-source", 100, toolCapability("spring.echo", 0));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateToolExtensionConfiguration {

        @Bean
        ExtensionSource firstSpringExtensionSource() {
            return source("first-source", 0, toolCapability("duplicate.tool", 0));
        }

        @Bean
        ExtensionSource secondSpringExtensionSource() {
            return source("second-source", 1, toolCapability("duplicate.tool", 0));
        }
    }

    private static ExtensionSource source(String sourceId, int order, ToolExtensionCapability... capabilities) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata(sourceId, sourceId, "1.0.0", "test", ExtensionApiVersion.parse("1.0.0"),
                        ExtensionCompatibility.supports("1.0.0", "2.0.0"), ExtensionLifecycleState.STARTED,
                        ExtensionHealth.up("ok"), true, Map.of("order", order, "sourceKind", "SPRING_BEAN"));
            }

            @Override
            public List<io.github.pi_java.agent.extension.api.ExtensionCapability> capabilities() {
                return List.of(capabilities);
            }
        };
    }

    private static ToolExtensionCapability toolCapability(String capabilityId, int order) {
        ToolProvenance provenance = new ToolProvenance(ToolProvenance.SourceKind.SPRING_BEAN, "spring-source",
                capabilityId, Map.of());
        ToolSchema schema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object"), Set.of(), 1024);
        ToolDescriptor descriptor = new ToolDescriptor(capabilityId, capabilityId, "Spring bean test tool", schema,
                Optional.empty(), provenance, "1.0.0", Set.of("test"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY,
                Duration.ofSeconds(5), Map.of());
        return new ToolExtensionCapability(capabilityId, descriptor,
                (request, cancellationToken) -> new ToolExecutionResult("call-test", capabilityId,
                        ToolExecutionStatus.SUCCESS, "ok", Optional.empty(), Map.of(), Map.of("ok", true), Set.of(),
                        Optional.empty(), Duration.ZERO, Optional.of(Map.of("ok", true))),
                Map.of("order", order, "sourceKind", "SPRING_BEAN"));
    }
}
