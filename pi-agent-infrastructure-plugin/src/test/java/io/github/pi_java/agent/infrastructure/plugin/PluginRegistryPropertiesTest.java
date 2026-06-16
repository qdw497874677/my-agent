package io.github.pi_java.agent.infrastructure.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginRegistryPropertiesTest {

    @Test
    void disabledDefaultsAreSafeAndConfigurationFileFirstForD03() {
        PluginRegistryProperties properties = PluginRegistryProperties.disabled();

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.pluginDirectory()).isEmpty();
        assertThat(properties.loadOnStartup()).isFalse();
        assertThat(properties.manualRefreshEnabled()).isTrue();
        assertThat(properties.allowedPluginIds()).isEmpty();
        assertThat(properties.selectedPluginIds()).isEmpty();
        assertThat(properties.platformApiVersion()).isEqualTo("1.0.0");
        assertThat(properties.allowDuplicateOverrides()).isFalse();
        assertThat(properties.nonSandboxWarningAcknowledged()).isFalse();
        assertThat(properties.validate()).isEmpty();
    }

    @Test
    void enabledRegistryRequiresControlledPluginDirectoryDeterministically() {
        PluginRegistryProperties properties = new PluginRegistryProperties(
                true, null, true, true, List.of(), List.of(), "1.0.0", false, true);

        assertThat(properties.validate())
                .containsExactly("pluginDirectory is required when controlled plugin loading is enabled");
        assertThatThrownBy(properties::requireValid)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginDirectory is required");
    }

    @Test
    void enabledRegistryExposesAllowlistSelectionCompatibilityAndSafetyWarningControls() {
        PluginRegistryProperties properties = new PluginRegistryProperties(
                true,
                Path.of("plugins/controlled"),
                true,
                false,
                List.of("audit-plugin", "tools-plugin"),
                List.of("audit-plugin"),
                "1.4.0",
                true,
                true);

        assertThat(properties.requireValid()).isSameAs(properties);
        assertThat(properties.pluginDirectory()).contains(Path.of("plugins/controlled"));
        assertThat(properties.allowedPluginIds()).containsExactly("audit-plugin", "tools-plugin");
        assertThat(properties.selectedPluginIds()).containsExactly("audit-plugin");
        assertThat(properties.allowDuplicateOverrides()).isTrue();
        assertThat(properties.nonSandboxWarningAcknowledged()).isTrue();
    }

    @Test
    void selectedPluginIdsMustBeInAllowlistWhenAllowlistConfigured() {
        PluginRegistryProperties properties = new PluginRegistryProperties(
                true,
                Path.of("plugins"),
                false,
                true,
                List.of("allowed"),
                List.of("allowed", "blocked"),
                "1.0.0",
                false,
                true);

        assertThat(properties.validate())
                .containsExactly("selectedPluginIds contains values outside allowedPluginIds: [blocked]");
    }
}
