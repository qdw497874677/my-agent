package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PluginGovernanceCatalogAdapterTest {

    @Test
    void disabledPluginsRemainVisibleAndDoNotContributeUsableCapabilities() {
        PluginStateStore store = new InMemoryPluginStateStore();
        store.disable("weather-plugin", "admin-1", "maintenance");
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered = discovered("weather-plugin", PluginState.STARTED,
                source("weather-source", capability("weather-cap")));

        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(List.of(discovered), store,
                new ExtensionRegistrationProperties());

        assertThat(adapter.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.pluginId()).isEqualTo("weather-plugin");
            assertThat(plugin.lifecycleStatus()).isEqualTo("DISABLED");
            assertThat(plugin.enabled()).isFalse();
            assertThat(plugin.reason()).isEqualTo("maintenance");
            assertThat(plugin.capabilities()).singleElement().satisfies(capability -> {
                assertThat(capability.status()).isEqualTo("DISABLED");
                assertThat(capability.enabled()).isFalse();
            });
        });
        assertThat(adapter.contributionRegistry().usableCapabilities()).isEmpty();
    }

    @Test
    void quarantinedPluginsRequireExplicitOperatorActionAndAreCapabilityInert() {
        PluginStateStore store = new InMemoryPluginStateStore();
        store.quarantine("risk-plugin", "admin-1", "repeated failure secret=raw");
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered = discovered("risk-plugin", PluginState.STARTED,
                source("risk-source", capability("risk-cap")));

        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(List.of(discovered), store,
                new ExtensionRegistrationProperties());

        PluginSourceStatus plugin = adapter.plugins().getFirst();
        assertThat(plugin.lifecycleStatus()).isEqualTo("QUARANTINED");
        assertThat(plugin.enabled()).isFalse();
        assertThat(plugin.reason()).contains("secret=<redacted>").doesNotContain("raw");
        assertThat(plugin.metadata()).containsEntry("operatorActionRequired", "true");
        assertThat(adapter.contributionRegistry().usableCapabilities()).isEmpty();
    }

    @Test
    void failedAndIncompatiblePluginsStayVisibleWithRedactedDiagnostics() {
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource failed = discoveredFailed("failed-plugin", PluginState.FAILED,
                new IllegalStateException("password=raw /srv/plugins/failed.jar"));
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource incompatible = incompatible("future-plugin");

        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(List.of(failed, incompatible),
                new InMemoryPluginStateStore(), new ExtensionRegistrationProperties());

        assertThat(adapter.plugins()).extracting(PluginSourceStatus::pluginId)
                .containsExactly("failed-plugin", "future-plugin");
        assertThat(adapter.plugins().getFirst().lifecycleStatus()).isEqualTo("FAILED");
        assertThat(adapter.plugins().getFirst().redactedError()).contains("password=<redacted>")
                .doesNotContain("raw")
                .doesNotContain("/srv/plugins");
        assertThat(adapter.plugins().get(1).compatibilityStatus()).isEqualTo("INCOMPATIBLE");
        assertThat(adapter.contributionRegistry().usableCapabilities()).isEmpty();
    }

    @Test
    void refreshDisableAndQuarantineMutationsReturnSanitizedStateTransitions() {
        InMemoryPluginStateStore store = new InMemoryPluginStateStore();
        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(List.of(), store,
                new ExtensionRegistrationProperties());

        PluginMutationStatus refresh = adapter.refresh();
        PluginMutationStatus disable = adapter.disable("plugin-1", "admin-1", "maintenance");
        PluginMutationStatus quarantine = adapter.quarantine("plugin-1", "admin-1", "token=raw suspicious");

        assertThat(refresh.operation()).isEqualTo("refresh");
        assertThat(disable.resultingLifecycleStatus()).isEqualTo("DISABLED");
        assertThat(quarantine.previousLifecycleStatus()).isEqualTo("DISABLED");
        assertThat(quarantine.resultingLifecycleStatus()).isEqualTo("QUARANTINED");
        assertThat(quarantine.metadata()).containsEntry("operatorActionRequired", "true");
        assertThat(quarantine.metadata().get("reason")).contains("token=<redacted>").doesNotContain("raw");
    }

    @Test
    void refreshUsesDiscoverySupplierAndReplacesCurrentContributionSnapshot() {
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource before = discovered("old-plugin", PluginState.STARTED,
                source("old-source", toolCapability("plugin.old.read")));
        Pf4jPluginSourceDiscovery.PluginDiscoveredSource after = discovered("new-plugin", PluginState.STARTED,
                source("new-source", toolCapability("plugin.new.read")));
        PluginRegistryProperties properties = new PluginRegistryProperties(
                true, java.util.Optional.of(Path.of("plugins")), true, true, List.of(), List.of(), "1.0.0", false, true);
        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(
                List.of(before), new InMemoryPluginStateStore(), new ExtensionRegistrationProperties(), properties,
                () -> List.of(after));

        assertThat(new DynamicPluginToolRegistry(adapter::contributionRegistry).resolve("plugin.old.read")).isPresent();

        PluginMutationStatus refresh = adapter.refresh();

        assertThat(refresh.applied()).isTrue();
        assertThat(refresh.status()).isEqualTo("REFRESHED");
        assertThat(refresh.metadata()).containsEntry("pluginCount", "1");
        assertThat(adapter.plugins()).extracting(PluginSourceStatus::pluginId).containsExactly("new-plugin");
        assertThat(new DynamicPluginToolRegistry(adapter::contributionRegistry).resolve("plugin.old.read")).isEmpty();
        assertThat(new DynamicPluginToolRegistry(adapter::contributionRegistry).resolve("plugin.new.read")).isPresent();
    }

    @Test
    void refreshReturnsDisabledStatusWhenManualRefreshIsDisabled() {
        PluginRegistryProperties properties = new PluginRegistryProperties(
                true, java.util.Optional.of(Path.of("plugins")), true, false, List.of(), List.of(), "1.0.0", false, true);
        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(
                List.of(), new InMemoryPluginStateStore(), new ExtensionRegistrationProperties(), properties, () -> {
                    throw new AssertionError("refresh supplier must not run when manual refresh is disabled");
                });

        PluginMutationStatus refresh = adapter.refresh();

        assertThat(refresh.applied()).isFalse();
        assertThat(refresh.operation()).isEqualTo("refresh");
        assertThat(refresh.status()).isEqualTo("REFRESH_DISABLED");
    }

    private static Pf4jPluginSourceDiscovery.PluginDiscoveredSource discovered(String pluginId, PluginState state,
                                                                               ExtensionSource source) {
        return new Pf4jPluginExtensionBridge(Path.of("/plugins"), "1.0.0")
                .bridge(wrapper(pluginId, state, "1.0.0", null), List.of(source))
                .getFirst();
    }

    private static Pf4jPluginSourceDiscovery.PluginDiscoveredSource discoveredFailed(String pluginId, PluginState state,
                                                                                     Throwable failure) {
        return new Pf4jPluginExtensionBridge(Path.of("/plugins"), "1.0.0")
                .bridge(wrapper(pluginId, state, "1.0.0", failure), List.of(source(pluginId + "-source", capability("ignored"))))
                .getFirst();
    }

    private static Pf4jPluginSourceDiscovery.PluginDiscoveredSource incompatible(String pluginId) {
        return new Pf4jPluginExtensionBridge(Path.of("/plugins"), "1.0.0")
                .bridge(wrapper(pluginId, PluginState.RESOLVED, "2.0.0", null), List.of(source("future-source", capability("future"))))
                .getFirst();
    }

    private static PluginWrapper wrapper(String id, PluginState state, String requires, Throwable failure) {
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor(id, id, "Plugin", "1.0.0", requires, "Pi", "Apache-2.0");
        PluginWrapper wrapper = new PluginWrapper(pluginManager(), descriptor, Path.of("/plugins/%s.jar".formatted(id)),
                PluginGovernanceCatalogAdapterTest.class.getClassLoader());
        wrapper.setPluginState(state);
        wrapper.setFailedException(failure);
        return wrapper;
    }

    private static PluginManager pluginManager() {
        return (PluginManager) Proxy.newProxyInstance(PluginGovernanceCatalogAdapterTest.class.getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if ("getRuntimeMode".equals(method.getName())) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    return switch (method.getReturnType().getName()) {
                        case "boolean" -> false;
                        case "int" -> 0;
                        default -> null;
                    };
                });
    }

    private static ExtensionSource source(String id, ExtensionCapability... capabilities) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata(id, id, "1.0.0", "test", ExtensionApiVersion.parse("1.0.0"),
                        ExtensionCompatibility.supports("1.0.0", "2.0.0"), ExtensionLifecycleState.STARTED,
                        ExtensionHealth.up("ok"), true, Map.of("order", 0));
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(capabilities);
            }
        };
    }

    private static ExtensionCapability capability(String id) {
        return new ExtensionCapability() {
            @Override
            public String capabilityId() {
                return id;
            }

            @Override
            public Type type() {
                return Type.TOOL;
            }

            @Override
            public Map<String, Object> redactedMetadata() {
                return Map.of("version", "1.0.0");
            }
        };
    }

    private static ExtensionCapability toolCapability(String id) {
        return new io.github.pi_java.agent.extension.api.ToolExtensionCapability(id,
                new io.github.pi_java.agent.domain.tool.ToolDescriptor(id, id, "Dynamic plugin test tool",
                        new io.github.pi_java.agent.domain.tool.ToolSchema("json-schema", Map.of("type", "object"), Set.of(), 4096),
                        java.util.Optional.empty(),
                        new io.github.pi_java.agent.domain.tool.ToolProvenance(
                                io.github.pi_java.agent.domain.tool.ToolProvenance.SourceKind.PLUGIN, id, id, Map.of()),
                        "1.0.0", Set.of("tool:plugin"), io.github.pi_java.agent.domain.tool.ToolRiskLevel.LOW,
                        io.github.pi_java.agent.domain.tool.ToolSideEffect.READ_ONLY, java.time.Duration.ofSeconds(2), Map.of()),
                (request, cancellationToken) -> new io.github.pi_java.agent.domain.tool.ToolExecutionResult(
                        request.toolCallId(), request.toolId(),
                        io.github.pi_java.agent.domain.tool.ToolExecutionStatus.SUCCESS, "ok", java.util.Optional.empty(),
                        Map.of(), Map.of("ok", true), Set.of(), java.util.Optional.empty(), java.time.Duration.ZERO,
                        java.util.Optional.of(Map.of("ok", true))),
                Map.of("version", "1.0.0", "sourceKind", "PLUGIN"));
    }
}
