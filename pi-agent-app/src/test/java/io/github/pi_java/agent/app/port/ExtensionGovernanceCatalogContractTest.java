package io.github.pi_java.agent.app.port;

import io.github.pi_java.agent.app.port.extension.EmptyExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionCapabilityStatus;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionGovernanceCatalogContractTest {

    @Test
    void extensionGovernanceCatalogIsReadOnlyAndFrameworkFree() throws NoSuchMethodException {
        Method sources = ExtensionGovernanceCatalog.class.getMethod("sources");

        assertThat(sources.getGenericReturnType().getTypeName())
                .isEqualTo("java.util.List<io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus>");
        assertThat(ExtensionGovernanceCatalog.class.getMethods())
                .extracting(Method::getName)
                .contains("sources", "overallStatus")
                .doesNotContain("enable", "disable", "register", "unregister");
    }

    @Test
    void emptyCatalogPreservesUnconfiguredStatus() {
        ExtensionGovernanceCatalog catalog = new EmptyExtensionGovernanceCatalog();

        assertThat(catalog.sources()).isEmpty();
        assertThat(catalog.overallStatus()).isEqualTo("UNCONFIGURED");
    }

    @Test
    void statusRecordsPreserveRedactedSourceAndCapabilityMetadata() {
        ExtensionCapabilityStatus capability = new ExtensionCapabilityStatus(
                "capability-1",
                "TOOL_PROVIDER",
                "FAILED",
                "1.0.0",
                "source-1",
                false,
                "INCOMPATIBLE",
                "UNHEALTHY",
                Map.of("secret", "<redacted>", "mutation", "disabled"));
        ExtensionSourceStatus source = new ExtensionSourceStatus(
                "source-1",
                "Broken SPI Source",
                "1.0.0",
                "JAVA_SPI",
                "FAILED",
                false,
                "INCOMPATIBLE",
                "UNHEALTHY",
                "<redacted>",
                List.of(capability));

        assertThat(source.capabilities()).containsExactly(capability);
        assertThat(source.redactedError()).isEqualTo("<redacted>");
        assertThat(capability.metadata()).containsEntry("secret", "<redacted>");
    }
}
