package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;
import io.github.pi_java.agent.infrastructure.plugin.InMemoryPluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.PluginGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.plugin.PluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.Pf4jPluginSourceDiscovery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

class SamplePluginJarCompatibilityE2ETest {

    @Test
    void samplePluginJarCompatibilityFailureRemainsVisibleButUnusable() throws IOException {
        Path pluginDirectory = SamplePluginJarE2ETest.prepareControlledPluginDirectory();
        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = discover(pluginDirectory, "0.5.0");

        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(discovered, new InMemoryPluginStateStore(),
                new ExtensionRegistrationProperties(Set.of(), Set.of(), false, "0.5.0"));

        assertThat(adapter.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.pluginId()).isEqualTo(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID);
            assertThat(plugin.compatibilityStatus()).isEqualTo("INCOMPATIBLE");
            assertThat(plugin.enabled()).isFalse();
            assertThat(plugin.relativePathSummary()).contains("pi-sample-plugin-readonly");
        });
        assertThat(new ExtensionToolRegistry(adapter.contributionRegistry()).resolve(SamplePluginJarE2ETest.SAMPLE_TOOL_ID)).isEmpty();
    }

    @Test
    void disableAndQuarantineStopNewSamplePluginToolResolution() throws IOException {
        Path pluginDirectory = SamplePluginJarE2ETest.prepareControlledPluginDirectory();
        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = discover(pluginDirectory, "1.0.0");
        InMemoryPluginStateStore stateStore = new InMemoryPluginStateStore();

        PluginGovernanceCatalogAdapter before = adapter(discovered, stateStore);
        assertThat(new ExtensionToolRegistry(before.contributionRegistry()).resolve(SamplePluginJarE2ETest.SAMPLE_TOOL_ID)).isPresent();

        stateStore.disable(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID, "admin-1", "maintenance");
        PluginGovernanceCatalogAdapter disabled = adapter(discovered, stateStore);
        assertThat(new ExtensionToolRegistry(disabled.contributionRegistry()).resolve(SamplePluginJarE2ETest.SAMPLE_TOOL_ID)).isEmpty();
        assertThat(disabled.plugins().getFirst().lifecycleStatus()).isEqualTo("DISABLED");

        stateStore.quarantine(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID, "admin-1", "suspicious plugin behavior");
        PluginGovernanceCatalogAdapter quarantined = adapter(discovered, stateStore);
        assertThat(new ExtensionToolRegistry(quarantined.contributionRegistry()).resolve(SamplePluginJarE2ETest.SAMPLE_TOOL_ID)).isEmpty();
        assertThat(quarantined.plugins().getFirst().lifecycleStatus()).isEqualTo("QUARANTINED");
        assertThat(quarantined.plugins().getFirst().metadata()).containsEntry("operatorActionRequired", "true");
    }

    private static PluginGovernanceCatalogAdapter adapter(List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered,
                                                          PluginStateStore stateStore) {
        return new PluginGovernanceCatalogAdapter(discovered, stateStore,
                new ExtensionRegistrationProperties(Set.of(), Set.of(), false, "1.0.0"));
    }

    private static List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discover(Path pluginDirectory,
                                                                                    String platformApiVersion) {
        PluginManager pluginManager = new DefaultPluginManager(List.of(pluginDirectory));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        return new Pf4jPluginSourceDiscovery(pluginManager, pluginDirectory, platformApiVersion).discover();
    }
}
