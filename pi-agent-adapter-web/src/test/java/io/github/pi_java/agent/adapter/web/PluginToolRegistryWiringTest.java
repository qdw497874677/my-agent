package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
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
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;
import io.github.pi_java.agent.infrastructure.plugin.InMemoryPluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.PluginCompatibilitySummary;
import io.github.pi_java.agent.infrastructure.plugin.PluginDescriptorSummary;
import io.github.pi_java.agent.infrastructure.plugin.PluginGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.plugin.PluginLifecycleSummary;
import io.github.pi_java.agent.infrastructure.plugin.Pf4jPluginSourceDiscovery;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, TestCloudRuntimeConfiguration.class,
        PluginToolRegistryWiringTest.FakePluginConfiguration.class}, properties = {
        "pi.plugins.enabled=true",
        "pi.plugins.directory=/tmp/pi-test-plugins",
        "pi.plugins.non-sandbox-warning-acknowledged=true",
        "pi.plugins.platform-api-version=1.0.0",
        "pi.plugins.allow-duplicate-overrides=false"
})
@ActiveProfiles("test")
class PluginToolRegistryWiringTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    @Qualifier("pluginToolRegistry")
    private ToolRegistry pluginToolRegistry;

    @Autowired
    private PluginGovernanceCatalog pluginGovernanceCatalog;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void pluginToolRegistryIsComposedAfterBuiltinsAndKeepsPluginProvenance() {
        assertThat(pluginToolRegistry.listTools()).extracting(ToolDescriptor::id).containsExactly("plugin.fake.read");

        assertThat(toolRegistry.listTools()).extracting(ToolDescriptor::id)
                .containsSubsequence("builtin.info", "builtin.workspace.write", "builtin.workspace.command", "plugin.fake.read");
        assertThat(toolRegistry.resolve("plugin.fake.read")).isPresent().get().satisfies(resolution -> {
            assertThat(resolution.descriptor().provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.PLUGIN);
            assertThat(resolution.descriptor().provenance().metadata())
                    .containsEntry("extension.sourceKind", "PLUGIN")
                    .containsEntry("extension.sourceId", "fake-plugin");
        });
    }

    @Test
    void pluginGovernanceCatalogExposesCompatibleUsablePluginWithoutRawPath() {
        assertThat(pluginGovernanceCatalog.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.pluginId()).isEqualTo("fake-plugin");
            assertThat(plugin.enabled()).isTrue();
            assertThat(plugin.relativePathSummary()).isEqualTo("fake-plugin.jar");
            assertThat(plugin.capabilities()).singleElement().satisfies(capability -> {
                assertThat(capability.capabilityId()).isEqualTo("plugin.fake.read");
                assertThat(capability.enabled()).isTrue();
            });
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakePluginConfiguration {
        @Bean
        @Primary
        PluginGovernanceCatalogAdapter fakePluginGovernanceCatalogAdapter(Clock clock) {
            InMemoryPluginStateStore stateStore = new InMemoryPluginStateStore(clock);
            return new PluginGovernanceCatalogAdapter(List.of(discoveredPlugin()), stateStore,
                    new io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties(
                            Set.of(), Set.of(), false, "1.0.0"));
        }

        private static Pf4jPluginSourceDiscovery.PluginDiscoveredSource discoveredPlugin() {
            ExtensionSource source = new ExtensionSource() {
                @Override
                public ExtensionMetadata metadata() {
                    return new ExtensionMetadata("fake-plugin", "Fake Plugin", "1.0.0", "Pi Test",
                            ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                            ExtensionLifecycleState.STARTED, ExtensionHealth.up("ok"), true,
                            Map.of("sourceKind", "PLUGIN"));
                }

                @Override
                public List<io.github.pi_java.agent.extension.api.ExtensionCapability> capabilities() {
                    return List.of(new ToolExtensionCapability("plugin.fake.read", descriptor(), binding(),
                            Map.of("sourceKind", "PLUGIN", "version", "1.0.0")));
                }
            };
            PluginDescriptorSummary descriptor = new PluginDescriptorSummary("fake-plugin", "Fake Plugin", "1.0.0",
                    "Pi Test", "fake-plugin.jar", new PluginCompatibilitySummary(true, "COMPATIBLE", ""), Map.of());
            PluginLifecycleSummary lifecycle = new PluginLifecycleSummary(ExtensionLifecycleState.STARTED, true, "");
            return new Pf4jPluginSourceDiscovery.PluginDiscoveredSource("fake-plugin", descriptor, lifecycle,
                    ServiceLoaderExtensionDiscovery.DiscoveredSource.discovered(source));
        }

        private static ToolDescriptor descriptor() {
            ToolSchema schema = new ToolSchema("json-schema-2020-12", Map.of("type", "object"), Set.of(), 4096);
            return new ToolDescriptor("plugin.fake.read", "Fake plugin read", "Safe read-only plugin tool", schema,
                    Optional.empty(), new ToolProvenance(ToolProvenance.SourceKind.PLUGIN, "fake-plugin",
                    "plugin.fake.read", Map.of()), "1.0.0", Set.of("tool:read"), ToolRiskLevel.LOW,
                    ToolSideEffect.NONE, Duration.ofSeconds(2), Map.of());
        }

        private static ToolExecutorBinding binding() {
            return (request, cancellationToken) -> new ToolExecutionResult(request.toolCallId(), request.toolId(),
                    ToolExecutionStatus.SUCCEEDED, "ok", Optional.empty(), request.arguments(), Map.of("ok", true),
                    Set.of(), Optional.empty(), Duration.ZERO, Optional.of(Map.of("ok", true)));
        }
    }
}
