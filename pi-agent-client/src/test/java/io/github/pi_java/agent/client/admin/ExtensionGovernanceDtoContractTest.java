package io.github.pi_java.agent.client.admin;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionGovernanceDtoContractTest {

    @Test
    void extensionGovernanceDtosArePublicRecordsWithRedactedMetadata() {
        ExtensionCapabilityDto capability = new ExtensionCapabilityDto(
                "capability-1",
                "TOOL_PROVIDER",
                "AVAILABLE",
                "1.0.0",
                "source-1",
                true,
                "COMPATIBLE",
                "HEALTHY",
                Map.of("mutation", "disabled", "secret", "<redacted>"));

        ExtensionSourceDto source = new ExtensionSourceDto(
                "source-1",
                "Test SPI Source",
                "1.0.0",
                "JAVA_SPI",
                "ACTIVE",
                true,
                "COMPATIBLE",
                "HEALTHY",
                "",
                List.of(capability));

        ExtensionGovernanceResponse response = new ExtensionGovernanceResponse(List.of(source));

        assertThat(ExtensionGovernanceResponse.class.isRecord()).isTrue();
        assertThat(ExtensionSourceDto.class.isRecord()).isTrue();
        assertThat(ExtensionCapabilityDto.class.isRecord()).isTrue();
        assertThat(response.sources()).containsExactly(source);
        assertThat(source.capabilities()).containsExactly(capability);
        assertThat(capability.metadata()).containsEntry("secret", "<redacted>");
        assertThat(componentTypes(ExtensionCapabilityDto.class))
                .contains(Map.class)
                .doesNotContain(Object.class);
    }

    private static List<Class<?>> componentTypes(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getType)
                .toList();
    }
}
