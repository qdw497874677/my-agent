package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PluginDescriptorSummaryTest {

    @Test
    void descriptorSummaryRedactsAbsolutePathsToControlledDirectoryRelativePath() {
        Path controlledDirectory = Path.of("/srv/pi/plugins");
        Path pluginJar = controlledDirectory.resolve("approved/audit-plugin.jar");

        PluginDescriptorSummary summary = PluginDescriptorSummary.fromControlledPath(
                "audit-plugin",
                "Audit Plugin",
                "1.2.3",
                "Pi Extensions",
                controlledDirectory,
                pluginJar,
                PluginCompatibilitySummary.compatible("[1.0.0,2.0.0)", "1.4.0"),
                Map.of("pluginClass", "com.example.SecretPlugin", "apiKey", "should-not-leak"));

        assertThat(summary.pluginId()).isEqualTo("audit-plugin");
        assertThat(summary.sourcePathSummary()).isEqualTo("approved/audit-plugin.jar");
        assertThat(summary.sourcePathSummary()).doesNotContain("/srv/pi");
        assertThat(summary.redactedMetadata()).containsEntry("metadataKeys", "apiKey,pluginClass");
        assertThat(summary.redactedMetadata()).doesNotContainEntry("apiKey", "should-not-leak");
    }

    @Test
    void descriptorSummaryFallsBackToFilenameOnlyForPathOutsideControlledDirectory() {
        PluginDescriptorSummary summary = PluginDescriptorSummary.fromControlledPath(
                "external-plugin",
                "External Plugin",
                "0.1.0",
                "Vendor",
                Path.of("/srv/pi/plugins"),
                Path.of("/tmp/uploads/external-plugin.jar"),
                PluginCompatibilitySummary.incompatible("[2.0.0,3.0.0)", "1.4.0"),
                Map.of());

        assertThat(summary.sourcePathSummary()).isEqualTo("external-plugin.jar");
        assertThat(summary.compatibility().compatible()).isFalse();
    }

    @Test
    void lifecycleSummaryCarriesLifecycleCompatibilityAndNonSandboxWarningWithoutUnsafeErrors() {
        PluginLifecycleSummary summary = PluginLifecycleSummary.failed(
                "audit-plugin",
                ExtensionLifecycleState.FAILED,
                PluginCompatibilitySummary.compatible("[1.0.0,2.0.0)", "1.4.0"),
                true,
                "IllegalStateException: token=abc123 secret path=/srv/pi/plugins/audit-plugin.jar");

        assertThat(summary.lifecycleState()).isEqualTo(ExtensionLifecycleState.FAILED);
        assertThat(summary.nonSandboxWarning()).isTrue();
        assertThat(summary.redactedError()).contains("<redacted>");
        assertThat(summary.redactedError()).doesNotContain("abc123");
        assertThat(summary.redactedError()).doesNotContain("/srv/pi/plugins");
    }
}
