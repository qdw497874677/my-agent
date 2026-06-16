package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpringExtensionConformanceTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PiAgentExtensionAutoConfiguration.class));

    @Test
    void springBeanSourceNormalizesToSameGovernanceSemanticsAsSpi() {
        contextRunner.withUserConfiguration(SpringConformanceConfiguration.class).run(context -> {
            ExtensionGovernanceCatalog governance = context.getBean(ExtensionGovernanceCatalog.class);

            assertThat(governance.sources()).singleElement().satisfies(source -> {
                assertThat(source.sourceId()).isEqualTo("phase6-conformance-extension");
                assertThat(source.kind()).isEqualTo("SPRING_BEAN");
                assertThat(source.lifecycleStatus()).isEqualTo("USABLE");
                assertThat(source.enabled()).isTrue();
                assertThat(source.compatibilityStatus()).isEqualTo("COMPATIBLE");
                assertThat(source.healthStatus()).isEqualTo("UP");
                assertThat(source.capabilities()).extracting(capability -> capability.type() + ":" + capability.capabilityId())
                        .containsExactly("TOOL:phase6.extension.safe-read", "TOOL:phase6.extension.workspace-write",
                                "EVENT_LISTENER:phase6.extension.audit-listener", "MODEL_PROVIDER:phase6.extension.provider",
                                "POLICY:phase6.extension.policy", "WORKSPACE_PROVIDER:phase6.extension.workspace",
                                "MEMORY_PROVIDER:phase6.extension.memory");
                assertThat(source.capabilities()).allSatisfy(capability -> {
                    assertThat(capability.status()).isEqualTo("USABLE");
                    assertThat(capability.metadata()).containsEntry("extension.sourceKind", "SPRING_BEAN");
                });
            });
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringConformanceConfiguration {
        @Bean
        ExtensionSource springConformanceExtensionSource() {
            return conformanceSource(ToolProvenance.SourceKind.SPRING_BEAN);
        }
    }

    static ExtensionSource conformanceSource(ToolProvenance.SourceKind sourceKind) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata("phase6-conformance-extension", "Phase 6 Conformance Extension", "1.0.0",
                        "Pi Test", ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                        ExtensionLifecycleState.STARTED, ExtensionHealth.up("ready"), true,
                        Map.of("sourceKind", sourceKind.name(), "order", 10));
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(cap("phase6.extension.safe-read", ExtensionCapability.Type.TOOL, sourceKind, 10),
                        cap("phase6.extension.workspace-write", ExtensionCapability.Type.TOOL, sourceKind, 20),
                        cap("phase6.extension.audit-listener", ExtensionCapability.Type.EVENT_LISTENER, sourceKind, 30),
                        cap("phase6.extension.provider", ExtensionCapability.Type.MODEL_PROVIDER, sourceKind, 40),
                        cap("phase6.extension.policy", ExtensionCapability.Type.POLICY, sourceKind, 50),
                        cap("phase6.extension.workspace", ExtensionCapability.Type.WORKSPACE_PROVIDER, sourceKind, 60),
                        cap("phase6.extension.memory", ExtensionCapability.Type.MEMORY_PROVIDER, sourceKind, 70));
            }
        };
    }

    private static ExtensionCapability cap(String id, ExtensionCapability.Type type, ToolProvenance.SourceKind sourceKind,
                                           int order) {
        return new ExtensionCapability() {
            @Override
            public String capabilityId() {
                return id;
            }

            @Override
            public Type type() {
                return type;
            }

            @Override
            public Map<String, Object> redactedMetadata() {
                return Map.of("sourceKind", sourceKind.name(), "order", order, "credentialRef", "env:PI_PHASE6_FAKE_SECRET",
                        "eventTypes", Set.of("tool.lifecycle"), "boundary", "ToolExecutionGateway");
            }
        };
    }
}
