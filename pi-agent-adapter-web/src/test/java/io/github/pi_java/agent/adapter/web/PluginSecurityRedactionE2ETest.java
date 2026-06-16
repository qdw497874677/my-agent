package io.github.pi_java.agent.adapter.web;

import org.junit.jupiter.api.Test;

class PluginSecurityRedactionE2ETest {

    @Test
    void rawPluginSecretsAreAbsentFromAllPublicSurfaces() {
        PluginRedactionFixture.missingUntilGreen();
    }
}
