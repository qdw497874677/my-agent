package io.github.pi_java.agent.adapter.web.ui.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminPluginGovernanceViewTest {

    @Test
    void consoleHttpClientExposesPluginGovernancePathsAndPublicDtoAnchorsOnly() {
        ConsoleHttpClient client = new ConsoleHttpClient();

        assertThat(client.adminPluginGovernancePath()).isEqualTo("/api/admin/governance/plugins");
        assertThat(client.adminPluginGovernanceResponseType()).isEqualTo(PluginGovernanceResponse.class);
        assertThat(client.adminPluginRefreshPath()).isEqualTo("/api/admin/governance/plugins/refresh");
        assertThat(client.adminPluginRefreshResponseType()).isEqualTo(PluginMutationResponse.class);
        assertThat(client.adminPluginDisablePath("plugin one")).isEqualTo("/api/admin/governance/plugins/plugin%20one/disable");
        assertThat(client.adminPluginQuarantinePath("plugin one")).isEqualTo("/api/admin/governance/plugins/plugin%20one/quarantine");
        assertThat(client.adminPluginMutationRequestType()).isEqualTo(PluginMutationRequest.class);
        assertThat(client.adminPluginMutationResponseType()).isEqualTo(PluginMutationResponse.class);

        assertThat(ConsoleHttpClient.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("adminPluginUploadPath", "adminPluginInstallPath", "adminPluginDeletePath",
                        "adminPluginUpgradePath", "adminPluginSearchPath", "adminPluginExportPath");
    }

    @Test
    void registryStatusViewRendersPluginMetadataActionsWarningsAndNoDeferredControls() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showPlugins(samplePluginGovernance());

        assertThat(view.pluginGovernancePath()).isEqualTo("/api/admin/governance/plugins");
        assertThat(view.pluginRefreshPath()).isEqualTo("/api/admin/governance/plugins/refresh");
        assertThat(view.pluginRefreshActionText()).contains("Refresh plugin discovery");
        assertThat(view.pluginDisableActionText("fake-plugin")).contains("Disable plugin")
                .contains("/api/admin/governance/plugins/fake-plugin/disable")
                .contains("confirmation required")
                .contains("optional reason");
        assertThat(view.pluginQuarantineActionText("fake-plugin")).contains("Quarantine plugin")
                .contains("/api/admin/governance/plugins/fake-plugin/quarantine")
                .contains("confirmation required")
                .contains("optional reason");

        assertThat(view.renderedText())
                .contains("Plugin governance warning: JVM classloader isolation is dependency/lifecycle isolation only; it is not a sandbox for untrusted code")
                .contains("Plugin: fake-plugin")
                .contains("name=Fake Plugin")
                .contains("version=1.2.3")
                .contains("vendor=Pi Test")
                .contains("sourceKind=PF4J_JAR")
                .contains("lifecycle=STARTED")
                .contains("enabled=true")
                .contains("health=UP")
                .contains("compatibility=COMPATIBLE")
                .contains("capabilities=2")
                .contains("USABLE=1")
                .contains("DISABLED=1")
                .contains("path=plugins/fake-plugin.jar")
                .contains("metadataKeys=safe")
                .contains("Plugin Capability: plugin.fake.read")
                .contains("type=TOOL")
                .contains("status=USABLE")
                .contains("Plugin: quarantined-plugin")
                .contains("lifecycle=QUARANTINED")
                .contains("reason=operator requested isolation")
                .contains("Plugin: failed-plugin")
                .contains("health=DOWN")
                .contains("compatibility=INCOMPATIBLE")
                .contains("error=[REDACTED] compatibility failure")
                .contains("Refresh plugin discovery via /api/admin/governance/plugins/refresh")
                .contains("Disable plugin fake-plugin via /api/admin/governance/plugins/fake-plugin/disable | confirmation required | optional reason")
                .contains("Quarantine plugin fake-plugin via /api/admin/governance/plugins/fake-plugin/quarantine | confirmation required | optional reason")
                .doesNotContain("PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK")
                .doesNotContain("/var/secret/plugins")
                .doesNotContain("Upload plugin")
                .doesNotContain("Install plugin")
                .doesNotContain("Delete plugin")
                .doesNotContain("Upgrade plugin")
                .doesNotContain("Search marketplace")
                .doesNotContain("Export plugin");
        assertThat(countElementsWithAttribute(view, "data-plugin-card")).isEqualTo(3);
        assertThat(countElementsWithAttribute(view, "data-plugin-capability-card")).isEqualTo(2);
        assertThat(countElementsWithAttributeValue(view, "data-plugin-card", "failed-plugin")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-plugin-card", "quarantined-plugin")).isEqualTo(1);
        assertThat(firstElementAttribute(view, "data-plugin-card")).isEqualTo("quarantined-plugin");
        assertThat(countElementsWithAttributeValue(view, "data-plugin-action", "refresh")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-plugin-action", "disable")).isEqualTo(3);
        assertThat(countElementsWithAttributeValue(view, "data-plugin-action", "quarantine")).isEqualTo(3);
        assertThat(countElementsWithAttributeValue(view, "data-plugin-warning", "not-a-sandbox")).isEqualTo(1);

        assertThat(view.mutationControlsPresent()).isTrue();
    }

    private static long countElementsWithAttribute(com.vaadin.flow.component.Component component, String attribute) {
        return elementStream(component).filter(element -> element.hasAttribute(attribute)).count();
    }

    private static long countElementsWithAttributeValue(com.vaadin.flow.component.Component component, String attribute, String value) {
        return elementStream(component).filter(element -> value.equals(element.getAttribute(attribute))).count();
    }

    private static String firstElementAttribute(com.vaadin.flow.component.Component component, String attribute) {
        return elementStream(component)
                .filter(element -> element.hasAttribute(attribute))
                .map(element -> element.getAttribute(attribute))
                .findFirst()
                .orElse("");
    }

    private static java.util.stream.Stream<com.vaadin.flow.dom.Element> elementStream(com.vaadin.flow.component.Component component) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(component.getElement()),
                component.getElement().getChildren().flatMap(AdminPluginGovernanceViewTest::descendants));
    }

    private static java.util.stream.Stream<com.vaadin.flow.dom.Element> descendants(com.vaadin.flow.dom.Element element) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(element),
                element.getChildren().flatMap(AdminPluginGovernanceViewTest::descendants));
    }

    private static PluginGovernanceResponse samplePluginGovernance() {
        PluginCapabilityDto readTool = new PluginCapabilityDto("plugin.fake.read", "TOOL", "USABLE", "1.0.0",
                "fake-plugin", true, "COMPATIBLE", "UP", Map.of("risk", "low"));
        PluginCapabilityDto disabledTool = new PluginCapabilityDto("plugin.fake.disabled", "TOOL", "DISABLED", "1.0.0",
                "fake-plugin", false, "COMPATIBLE", "UP", Map.of("reason", "disabled by fixture"));
        PluginSourceDto healthy = new PluginSourceDto("fake-plugin", "Fake Plugin", "1.2.3", "Pi Test",
                "PF4J_JAR", "STARTED", true, "UP", "COMPATIBLE", 2,
                Map.of("USABLE", "1", "DISABLED", "1"), "", "plugins/fake-plugin.jar", "",
                Instant.parse("2026-06-16T12:00:00Z"), List.of(readTool, disabledTool),
                Map.of("metadataKeys", "safe"));
        PluginSourceDto quarantined = new PluginSourceDto("quarantined-plugin", "Quarantined Plugin", "2.0.0", "Pi Test",
                "PF4J_JAR", "QUARANTINED", false, "WARN", "COMPATIBLE", 0,
                Map.of(), "", "plugins/quarantined-plugin.jar", "operator requested isolation",
                Instant.parse("2026-06-16T12:01:00Z"), List.of(), Map.of());
        PluginSourceDto failed = new PluginSourceDto("failed-plugin", "Failed Plugin", "9.9.9", "Pi Test",
                "PF4J_JAR", "FAILED", false, "DOWN", "INCOMPATIBLE", 0,
                Map.of(), "[REDACTED] compatibility failure", "plugins/failed-plugin.jar", "",
                Instant.parse("2026-06-16T12:02:00Z"), List.of(), Map.of("operatorHint", "check API version"));
        return new PluginGovernanceResponse(List.of(healthy, quarantined, failed));
    }
}
