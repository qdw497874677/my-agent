package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.port.plugin.EmptyPluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginGovernanceQueryServiceTest {

    @Test
    void pluginGovernancePortAndDtosDefensivelyCopyCollections() {
        Map<String, String> capabilityMetadata = new HashMap<>();
        capabilityMetadata.put("policy", "read-only");
        PluginCapabilityStatus capability = new PluginCapabilityStatus(
                "plugin-1:tool.weather",
                "TOOL_PROVIDER",
                "AVAILABLE",
                "1.0.0",
                "plugin-1",
                true,
                "COMPATIBLE",
                "HEALTHY",
                capabilityMetadata);
        List<PluginCapabilityStatus> capabilities = new ArrayList<>();
        capabilities.add(capability);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("descriptor", "<redacted>");
        PluginSourceStatus source = new PluginSourceStatus(
                "plugin-1",
                "Weather Plugin",
                "1.0.0",
                "Pi Java",
                "PF4J_JAR",
                "STARTED",
                true,
                "HEALTHY",
                "COMPATIBLE",
                1,
                Map.of("TOOL_PROVIDER", "1"),
                "",
                "plugins/weather-plugin.jar",
                "operator-approved",
                Instant.parse("2026-06-16T12:00:00Z"),
                capabilities,
                metadata);

        capabilities.clear();
        metadata.put("raw", "should-not-appear");
        capabilityMetadata.put("secret", "should-not-appear");

        assertThat(source.capabilities()).containsExactly(capability);
        assertThat(source.metadata()).containsEntry("descriptor", "<redacted>")
                .doesNotContainKey("raw");
        assertThat(source.capabilities().getFirst().metadata()).containsEntry("policy", "read-only")
                .doesNotContainKey("secret");

        PluginSourceDto sourceDto = new PluginSourceDto(
                source.pluginId(), source.name(), source.version(), source.vendor(), source.sourceKind(),
                source.lifecycleStatus(), source.enabled(), source.healthStatus(), source.compatibilityStatus(),
                source.capabilityCount(), source.capabilityStatusCounts(), source.redactedError(),
                source.relativePathSummary(), source.reason(), source.lastUpdatedAt(),
                List.of(new PluginCapabilityDto(
                        capability.capabilityId(), capability.type(), capability.status(), capability.version(),
                        capability.pluginId(), capability.enabled(), capability.compatibilityStatus(),
                        capability.healthStatus(), capability.metadata())),
                source.metadata());
        PluginGovernanceResponse response = new PluginGovernanceResponse(List.of(sourceDto));

        assertThat(response.plugins()).hasSize(1);
        assertThat(response.toString()).doesNotContain("should-not-appear");
    }

    @Test
    void pluginMutationContractsSupportOnlyRefreshDisableAndQuarantine() {
        PluginGovernanceCatalog catalog = new EmptyPluginGovernanceCatalog();

        PluginMutationStatus refresh = catalog.refresh();
        PluginMutationStatus disable = catalog.disable("plugin-1", "admin-1", "maintenance");
        PluginMutationStatus quarantine = catalog.quarantine("plugin-1", "admin-1", "compatibility failure");

        assertThat(refresh.operation()).isEqualTo("refresh");
        assertThat(disable.operation()).isEqualTo("disable");
        assertThat(quarantine.operation()).isEqualTo("quarantine");
        assertThat(catalog.plugins()).isEmpty();

        PluginMutationRequest request = new PluginMutationRequest("quarantine", "compatibility failure");
        PluginMutationResponse response = new PluginMutationResponse(
                true,
                "plugin-1",
                request.operation(),
                "STARTED",
                "QUARANTINED",
                "QUARANTINED",
                "",
                Map.of("actor", "admin-1"));

        assertThat(request.operation()).isEqualTo("quarantine");
        assertThat(response.operation()).isEqualTo("quarantine");
        assertThat(response.metadata()).containsEntry("actor", "admin-1");
    }
}
