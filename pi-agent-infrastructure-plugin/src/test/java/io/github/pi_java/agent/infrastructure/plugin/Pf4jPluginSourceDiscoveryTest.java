package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;
import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class Pf4jPluginSourceDiscoveryTest {

    @Test
    void bridgesPluginExtensionSourcesAndAddsPf4jDescriptorMetadata() {
        PluginWrapper wrapper = wrapper("sample-plugin", "Sample Plugin", "1.2.3", "Pi Team", PluginState.STARTED, null);
        Pf4jPluginExtensionBridge bridge = new Pf4jPluginExtensionBridge(Path.of("/plugins"), "1.0.0");

        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = bridge.bridge(wrapper,
                List.of(source("source-one", capability("cap-one")), source("source-two", capability("cap-two"))));

        assertThat(discovered).hasSize(2);
        assertThat(discovered).extracting(Pf4jPluginSourceDiscovery.PluginDiscoveredSource::pluginId)
                .containsExactly("sample-plugin", "sample-plugin");
        assertThat(discovered.getFirst().descriptor()).satisfies(descriptor -> {
            assertThat(descriptor.pluginId()).isEqualTo("sample-plugin");
            assertThat(descriptor.name()).isEqualTo("Sample Plugin");
            assertThat(descriptor.version()).isEqualTo("1.2.3");
            assertThat(descriptor.provider()).isEqualTo("Pi Team");
            assertThat(descriptor.sourcePathSummary()).isEqualTo("sample-plugin.jar");
        });
        ExtensionSource enriched = discovered.getFirst().discoveredSource().source().orElseThrow();
        assertThat(enriched.metadata().redactedMetadata()).containsEntry("sourceKind", "PLUGIN")
                .containsEntry("plugin.id", "sample-plugin")
                .containsEntry("plugin.version", "1.2.3")
                .containsEntry("plugin.provider", "Pi Team")
                .containsEntry("plugin.sourcePath", "sample-plugin.jar");
        assertThat(enriched.capabilities()).singleElement().satisfies(capability -> assertThat(capability.redactedMetadata())
                .containsEntry("sourceKind", "PLUGIN")
                .containsEntry("plugin.id", "sample-plugin"));

        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(
                discovered.stream().map(Pf4jPluginSourceDiscovery.PluginDiscoveredSource::discoveredSource).toList(),
                new ExtensionRegistrationProperties());
        assertThat(registry.usableCapabilities()).extracting(DefaultExtensionContributionRegistry.CapabilityEntry::capabilityId)
                .containsExactly("cap-one", "cap-two");
    }

    @Test
    void descriptorFailureRemainsVisibleAndContributesNoUsableCapabilities() {
        PluginWrapper wrapper = wrapper("bad-plugin", "Bad Plugin", "9.9.9", "Ops", PluginState.FAILED,
                new IllegalStateException("secret=raw path=/srv/plugins/bad-plugin.jar"));
        Pf4jPluginExtensionBridge bridge = new Pf4jPluginExtensionBridge(Path.of("/plugins"), "1.0.0");

        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = bridge.bridge(wrapper,
                List.of(source("ignored-source", capability("ignored-cap"))));

        assertThat(discovered).singleElement().satisfies(plugin -> {
            assertThat(plugin.lifecycle().lifecycleState()).isEqualTo(ExtensionLifecycleState.FAILED);
            assertThat(plugin.lifecycle().redactedError()).contains("secret=<redacted>")
                    .doesNotContain("raw")
                    .doesNotContain("/srv/plugins");
            assertThat(plugin.discoveredSource().status()).isEqualTo(ServiceLoaderExtensionDiscovery.DiscoveryStatus.FAILED);
            assertThat(plugin.discoveredSource().source()).isEmpty();
        });
        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(
                discovered.stream().map(Pf4jPluginSourceDiscovery.PluginDiscoveredSource::discoveredSource).toList(),
                new ExtensionRegistrationProperties());
        assertThat(registry.sourceEntries()).singleElement().satisfies(source -> {
            assertThat(source.sourceId()).isEqualTo("bad-plugin");
            assertThat(source.status()).isEqualTo("FAILED");
        });
        assertThat(registry.usableCapabilities()).isEmpty();
    }

    private static PluginWrapper wrapper(String id, String description, String version, String provider,
                                         PluginState state, Throwable failure) {
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor(id, description,
                "io.github.pi_java.agent.test.Plugin", version, "1.0.0", provider, "Apache-2.0");
        PluginWrapper wrapper = new PluginWrapper(null, descriptor, Path.of("/plugins/%s.jar".formatted(id)),
                Pf4jPluginSourceDiscoveryTest.class.getClassLoader());
        wrapper.setPluginState(state);
        wrapper.setFailedException(failure);
        return wrapper;
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
}
