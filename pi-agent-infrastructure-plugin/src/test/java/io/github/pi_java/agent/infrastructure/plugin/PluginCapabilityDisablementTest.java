package io.github.pi_java.agent.infrastructure.plugin;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PluginCapabilityDisablementTest {

    @Test
    void disabledPluginToolCannotBeResolvedForNewRegistryLookupAfterDisableMutation() {
        InMemoryPluginStateStore stateStore = new InMemoryPluginStateStore();
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered = discovered("weather-plugin", "plugin.weather.read");
        PluginGovernanceCatalogAdapter before = adapter(List.of(discovered), stateStore);

        assertThat(new ExtensionToolRegistry(before.contributionRegistry()).resolve("plugin.weather.read")).isPresent();

        stateStore.disable("weather-plugin", "admin-1", "maintenance secret=raw");
        PluginGovernanceCatalogAdapter after = adapter(List.of(discovered), stateStore);

        assertThat(new ExtensionToolRegistry(after.contributionRegistry()).resolve("plugin.weather.read")).isEmpty();
        assertThat(after.contributionRegistry().usableCapabilities()).isEmpty();
        assertThat(after.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.lifecycleStatus()).isEqualTo("DISABLED");
            assertThat(plugin.enabled()).isFalse();
            assertThat(plugin.reason()).contains("secret=<redacted>").doesNotContain("raw");
            assertThat(plugin.capabilities()).singleElement().satisfies(capability -> {
                assertThat(capability.capabilityId()).isEqualTo("plugin.weather.read");
                assertThat(capability.status()).isEqualTo("DISABLED");
                assertThat(capability.enabled()).isFalse();
            });
        });
    }

    @Test
    void quarantinedPluginToolCannotBeResolvedAndGovernanceShowsOperatorReason() {
        InMemoryPluginStateStore stateStore = new InMemoryPluginStateStore();
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered = discovered("risk-plugin", "plugin.risk.read");

        stateStore.quarantine("risk-plugin", "admin-1", "operator isolation path=/var/secret/plugins/risk.jar");
        PluginGovernanceCatalogAdapter adapter = adapter(List.of(discovered), stateStore);

        assertThat(new ExtensionToolRegistry(adapter.contributionRegistry()).listTools()).isEmpty();
        assertThat(new ExtensionToolRegistry(adapter.contributionRegistry()).resolve("plugin.risk.read")).isEmpty();
        assertThat(adapter.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.lifecycleStatus()).isEqualTo("QUARANTINED");
            assertThat(plugin.enabled()).isFalse();
            assertThat(plugin.reason()).contains("operator isolation").doesNotContain("/var/secret/plugins");
            assertThat(plugin.metadata()).containsEntry("operatorActionRequired", "true");
            assertThat(plugin.capabilities()).singleElement().satisfies(capability -> {
                assertThat(capability.status()).isEqualTo("QUARANTINED");
                assertThat(capability.enabled()).isFalse();
            });
        });
    }

    private static PluginGovernanceCatalogAdapter adapter(List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered,
                                                          InMemoryPluginStateStore stateStore) {
        return new PluginGovernanceCatalogAdapter(discovered, stateStore, new ExtensionRegistrationProperties());
    }

    private static Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered(String pluginId, String capabilityId) {
        ExtensionSource source = new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata(pluginId, pluginId, "1.0.0", "Pi Test",
                        ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                        ExtensionLifecycleState.STARTED, ExtensionHealth.up("ok"), true, Map.of("sourceKind", "PLUGIN"));
            }

            @Override
            public List<io.github.pi_java.agent.extension.api.ExtensionCapability> capabilities() {
                return List.of(new ToolExtensionCapability(capabilityId, descriptor(pluginId, capabilityId), binding(),
                        Map.of("sourceKind", "PLUGIN", "version", "1.0.0")));
            }
        };
        PluginCompatibilitySummary compatibility = PluginCompatibilitySummary.compatible("1.0.0..2.0.0", "1.0.0");
        PluginDescriptorSummary descriptor = new PluginDescriptorSummary(pluginId, pluginId, "1.0.0", "Pi Test",
                pluginId + ".jar", compatibility, Map.of());
        PluginLifecycleSummary lifecycle = PluginLifecycleSummary.of(pluginId, ExtensionLifecycleState.STARTED, compatibility, true);
        return new Pf4jPluginSourceDiscovery.PluginDiscoveredSource(pluginId, descriptor, lifecycle,
                ServiceLoaderExtensionDiscovery.DiscoveredSource.discovered(source));
    }

    private static ToolDescriptor descriptor(String pluginId, String capabilityId) {
        return new ToolDescriptor(capabilityId, capabilityId, "Plugin disablement regression tool",
                new ToolSchema("json-schema", Map.of("type", "object"), Set.of(), 4096), Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.PLUGIN, pluginId, capabilityId, Map.of()), "1.0.0",
                Set.of("tool:plugin"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(2), Map.of());
    }

    private static io.github.pi_java.agent.app.port.tool.ToolExecutorBinding binding() {
        return (request, cancellationToken) -> new ToolExecutionResult(request.toolCallId(), request.toolId(),
                ToolExecutionStatus.SUCCESS, "ok", Optional.empty(), Map.of(), Map.of("ok", true), Set.of(),
                Optional.empty(), Duration.ZERO, Optional.of(Map.of("ok", true)));
    }
}
