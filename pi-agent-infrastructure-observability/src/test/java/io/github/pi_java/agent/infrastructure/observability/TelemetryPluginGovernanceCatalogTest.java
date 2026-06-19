package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginMutationStatus;
import io.github.pi_java.agent.app.port.plugin.PluginSourceStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryPluginGovernanceCatalogTest {
    private static final String SECRET_PATH = "/tmp/PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK/plugin.jar";

    @Test
    void records_plugin_lifecycle_without_absolute_path_or_metadata_leakage() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryPluginGovernanceCatalog catalog = new TelemetryPluginGovernanceCatalog(delegate(), new PiTelemetry(registry, null));

        catalog.plugins();
        catalog.refresh();
        catalog.disable("plugin-a", "operator", "because " + SECRET_PATH);
        catalog.quarantine("plugin-a", "operator", "because " + SECRET_PATH);

        assertThat(registry.get(PiTelemetryNames.PLUGIN_LIFECYCLE_TOTAL)
                .tags("plugin_id", "all", "action", "plugins", "status", "success", "selection_status", "NOT_SELECTED")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(PiTelemetryNames.PLUGIN_DISCOVERY_DURATION)
                .tags("plugin_id", "all", "action", "refresh", "status", "REFRESHED", "selection_status", "unknown")
                .timer().count()).isEqualTo(1L);
        assertThat(allMeterTagValues(registry)).doesNotContain(SECRET_PATH, "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK", "secretMetadata");
    }

    private static PluginGovernanceCatalog delegate() {
        return new PluginGovernanceCatalog() {
            @Override
            public List<PluginSourceStatus> plugins() {
                return List.of(new PluginSourceStatus("plugin-a", "Plugin A", "1.0.0", "vendor", "PF4J_JAR",
                        "STARTED", true, "UP", "COMPATIBLE", 0, Map.of(), "", SECRET_PATH, "",
                        Instant.EPOCH, List.of(), Map.of("selectionStatus", "NOT_SELECTED", "secretMetadata", SECRET_PATH)));
            }

            @Override
            public PluginMutationStatus refresh() {
                return new PluginMutationStatus(true, "", "refresh", "", "", "REFRESHED", SECRET_PATH,
                        Map.of("path", SECRET_PATH));
            }

            @Override
            public PluginMutationStatus disable(String pluginId, String actor, String reason) {
                return new PluginMutationStatus(true, pluginId, "disable", "STARTED", "DISABLED", "DISABLED", SECRET_PATH,
                        Map.of("reason", reason));
            }

            @Override
            public PluginMutationStatus quarantine(String pluginId, String actor, String reason) {
                return new PluginMutationStatus(true, pluginId, "quarantine", "DISABLED", "QUARANTINED", "QUARANTINED",
                        SECRET_PATH, Map.of("reason", reason));
            }
        };
    }

    private static String allMeterTagValues(SimpleMeterRegistry registry) {
        StringBuilder values = new StringBuilder();
        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> values.append(tag.getValue()).append('\n'));
        }
        return values.toString();
    }
}
