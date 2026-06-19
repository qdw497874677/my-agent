package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.port.plugin.EmptyPluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginCapabilityStatus;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginGovernanceQueryServiceTest {

    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-1", "admin-1", Set.of("ADMIN")),
            new CorrelationContext("trace-1", "corr-1", null));

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

    @Test
    void pluginsQueryMapsCatalogSourcesToPublicDtosWithoutRawErrors() {
        DefaultGovernanceQueryService service = serviceWith(fakeCatalog());

        PluginGovernanceResponse response = service.plugins(CONTEXT);

        assertThat(response.plugins()).hasSize(3);
        assertThat(response.plugins().getFirst().pluginId()).isEqualTo("plugin-healthy");
        assertThat(response.plugins().getFirst().capabilities()).hasSize(1);
        assertThat(response.plugins().get(1).lifecycleStatus()).isEqualTo("DISABLED");
        assertThat(response.plugins().get(2).lifecycleStatus()).isEqualTo("QUARANTINED");
        assertThat(response.toString()).doesNotContain("raw-secret");
    }

    @Test
    void overviewPluginStatusReflectsCatalogCountsAndRemovesPlaceholder() {
        DefaultGovernanceQueryService service = serviceWith(fakeCatalog());

        GovernanceOverviewResponse overview = service.overview(CONTEXT);

        assertThat(overview.plugins().status()).isEqualTo("QUARANTINED");
        assertThat(overview.plugins().message()).isEqualTo("Plugin governance catalog is available");
        assertThat(overview.plugins().count()).isEqualTo(3);
        assertThat(overview.plugins().metadata())
                .containsEntry("surface", "plugin-governance")
                .containsEntry("mutation", "refresh,disable,quarantine")
                .containsEntry("plugins", "3")
                .containsEntry("capabilities", "2")
                .containsEntry("disabledPlugins", "1")
                .containsEntry("quarantinedPlugins", "1")
                .containsEntry("incompatiblePlugins", "1")
                .containsEntry("failedPlugins", "1");
        assertThat(overview.plugins().metadata()).doesNotContainEntry("surface", "placeholder");
    }

    @Test
    void pluginMutationsDelegateToCatalogWithRequestActorAndReason() {
        RecordingCatalog catalog = new RecordingCatalog();
        DefaultGovernanceQueryService service = serviceWith(catalog);

        PluginMutationResponse refresh = service.refreshPlugins(CONTEXT);
        PluginMutationResponse disable = service.disablePlugin(CONTEXT, "plugin-1", new PluginMutationRequest("disable", "maintenance"));
        PluginMutationResponse quarantine = service.quarantinePlugin(CONTEXT, "plugin-1", new PluginMutationRequest("quarantine", "bad health"));

        assertThat(refresh.operation()).isEqualTo("refresh");
        assertThat(disable.resultingLifecycleStatus()).isEqualTo("DISABLED");
        assertThat(quarantine.resultingLifecycleStatus()).isEqualTo("QUARANTINED");
        assertThat(catalog.lastActor).isEqualTo("admin-1");
        assertThat(catalog.lastReason).isEqualTo("bad health");
    }

    @Test
    void emptyCatalogMapsToUnconfiguredPluginOverview() {
        DefaultGovernanceQueryService service = serviceWith(new EmptyPluginGovernanceCatalog());

        GovernanceOverviewResponse overview = service.overview(CONTEXT);

        assertThat(service.plugins(CONTEXT).plugins()).isEmpty();
        assertThat(overview.plugins().status()).isEqualTo("UNCONFIGURED");
        assertThat(overview.plugins().message()).isEqualTo("No plugin sources are configured");
        assertThat(overview.plugins().metadata()).containsEntry("plugins", "0");
    }

    private static DefaultGovernanceQueryService serviceWith(PluginGovernanceCatalog pluginCatalog) {
        return new DefaultGovernanceQueryService(
                List::of,
                new ToolRegistry() {
                    @Override
                    public List<io.github.pi_java.agent.domain.tool.ToolDescriptor> listTools() {
                        return List.of();
                    }

                    @Override
                    public Optional<ToolResolution> resolve(String toolId) {
                        return Optional.empty();
                    }
                },
                () -> List.of(),
                new McpGovernanceCatalog.EmptyMcpGovernanceCatalog(),
                pluginCatalog,
                Optional.empty(),
                Optional.empty(),
                Clock.fixed(Instant.parse("2026-06-16T12:00:00Z"), ZoneOffset.UTC));
    }

    private static PluginGovernanceCatalog fakeCatalog() {
        PluginCapabilityStatus capability = new PluginCapabilityStatus(
                "plugin-healthy:tool.weather", "TOOL_PROVIDER", "AVAILABLE", "1.0.0", "plugin-healthy",
                true, "COMPATIBLE", "HEALTHY", Map.of("secret", "<redacted>"));
        PluginSourceStatus healthy = new PluginSourceStatus(
                "plugin-healthy", "Weather Plugin", "1.0.0", "Pi Java", "PF4J_JAR", "STARTED",
                true, "HEALTHY", "COMPATIBLE", 1, Map.of("AVAILABLE", "1"), "",
                "plugins/weather.jar", "", Instant.parse("2026-06-16T12:00:00Z"), List.of(capability),
                Map.of("descriptor", "<redacted>"));
        PluginSourceStatus disabled = new PluginSourceStatus(
                "plugin-disabled", "Disabled Plugin", "1.0.0", "Pi Java", "PF4J_JAR", "DISABLED",
                false, "HEALTHY", "COMPATIBLE", 0, Map.of(), "",
                "plugins/disabled.jar", "operator maintenance", Instant.parse("2026-06-16T12:00:00Z"), List.of(),
                Map.of("reason", "operator maintenance"));
        PluginSourceStatus quarantined = new PluginSourceStatus(
                "plugin-quarantined", "Broken Plugin", "1.0.0", "Pi Java", "PF4J_JAR", "QUARANTINED",
                false, "UNHEALTHY", "INCOMPATIBLE", 1, Map.of("FAILED", "1"), "error:<redacted>",
                "plugins/broken.jar", "compatibility failure", Instant.parse("2026-06-16T12:00:00Z"),
                List.of(new PluginCapabilityStatus(
                        "plugin-quarantined:tool.broken", "TOOL_PROVIDER", "FAILED", "1.0.0", "plugin-quarantined",
                        false, "INCOMPATIBLE", "UNHEALTHY", Map.of("error", "<redacted>"))),
                Map.of("raw", "<redacted>"));
        return new PluginGovernanceCatalog() {
            @Override
            public List<PluginSourceStatus> plugins() {
                return List.of(healthy, disabled, quarantined);
            }

            @Override
            public PluginMutationStatus refresh() {
                return new PluginMutationStatus(true, "", "refresh", "", "", "QUARANTINED", "", Map.of());
            }

            @Override
            public PluginMutationStatus disable(String pluginId, String actor, String reason) {
                return new PluginMutationStatus(true, pluginId, "disable", "STARTED", "DISABLED", "DISABLED", "",
                        Map.of("actor", actor, "reason", reason));
            }

            @Override
            public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
                return new PluginMutationStatus(true, pluginId, "quarantine", "STARTED", "QUARANTINED", "QUARANTINED", "",
                        Map.of("actor", actor, "reason", reason));
            }
        };
    }

    private static final class RecordingCatalog implements PluginGovernanceCatalog {
        private String lastActor;
        private String lastReason;

        @Override
        public List<PluginSourceStatus> plugins() {
            return List.of();
        }

        @Override
        public PluginMutationStatus refresh() {
            return new PluginMutationStatus(true, "", "refresh", "", "", "HEALTHY", "", Map.of());
        }

        @Override
        public PluginMutationStatus disable(String pluginId, String actor, String reason) {
            lastActor = actor;
            lastReason = reason;
            return new PluginMutationStatus(true, pluginId, "disable", "STARTED", "DISABLED", "DISABLED", "", Map.of());
        }

        @Override
        public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
            lastActor = actor;
            lastReason = reason;
            return new PluginMutationStatus(true, pluginId, "quarantine", "STARTED", "QUARANTINED", "QUARANTINED", "", Map.of());
        }
    }
}
