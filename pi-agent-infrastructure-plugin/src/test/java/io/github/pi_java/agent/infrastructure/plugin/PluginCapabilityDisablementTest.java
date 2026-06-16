package io.github.pi_java.agent.infrastructure.plugin;

import org.junit.jupiter.api.Test;

class PluginCapabilityDisablementTest {

    @Test
    void disabledAndQuarantinedPluginsAreCapabilityInertForNewResolution() {
        PluginDisablementFixture.missingUntilGreen();
    }
}
