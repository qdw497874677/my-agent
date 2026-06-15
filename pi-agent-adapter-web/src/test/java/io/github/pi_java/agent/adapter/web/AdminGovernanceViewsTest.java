package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminGovernanceOverviewView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminAuditView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminPolicyDecisionsView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminRegistryStatusView;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminGovernanceViewsTest {

    @Test
    void overviewShowsRuntimeRegistryGovernanceAndFuturePlaceholders() {
        AdminGovernanceOverviewView view = new AdminGovernanceOverviewView(new ConsoleHttpClient());
        view.showOverview(sampleOverview());

        assertThat(view.overviewPath()).isEqualTo("/api/admin/governance/overview");
        assertThat(view.registryStatusPath()).isEqualTo("/admin/governance/registry");
        assertThat(view.renderedText())
                .contains("Runtime")
                .contains("HEALTHY")
                .contains("Model Providers")
                .contains("Tool Registry")
                .contains("Extensions")
                .contains("FUTURE_ENABLED")
                .contains("MCP")
                .contains("UNCONFIGURED")
                .contains("Plugins")
                .contains("Policy decisions")
                .contains("Audit summaries");
        assertThat(view.getElement().getAttribute("data-admin-surface")).isEqualTo("inspect-only");
    }

    @Test
    void registryStatusViewIsReadOnlyAndShowsProviderToolExtensionMcpPluginStatus() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showOverview(sampleOverview());

        assertThat(view.overviewPath()).isEqualTo("/api/admin/governance/overview");
        assertThat(view.extensionGovernancePath()).isEqualTo("/api/admin/governance/extensions");
        assertThat(view.renderedText())
                .contains("Model Providers: HEALTHY")
                .contains("Tool Registry: HEALTHY")
                .contains("Extensions: FUTURE_ENABLED")
                .contains("MCP: UNCONFIGURED")
                .contains("Plugins: FUTURE_ENABLED")
                .contains("mutation=disabled")
                .contains("surface=placeholder");
        assertThat(view.mutationControlsPresent()).isFalse();
        assertThat(view.mutationActionText().toLowerCase())
                .doesNotContain("enable button")
                .doesNotContain("disable")
                .doesNotContain("delete")
                .doesNotContain("load plugin")
                .doesNotContain("add mcp");
    }

    @Test
    void registryStatusViewShowsReadOnlyExtensionSourcesAndCapabilities() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showExtensions(sampleExtensions());

        assertThat(view.renderedText())
                .contains("Extension Source: test-spring-extension")
                .contains("kind=SPRING_BEAN")
                .contains("status=USABLE")
                .contains("health=UP")
                .contains("compatibility=COMPATIBLE")
                .contains("enabled=true")
                .contains("Capability: listener.audit")
                .contains("type=EVENT_LISTENER")
                .contains("extension.sourceKind=SPRING_BEAN")
                .contains("[REDACTED]")
                .doesNotContain("enable button")
                .doesNotContain("disable button")
                .doesNotContain("delete");
        assertThat(view.mutationControlsPresent()).isFalse();
    }

    @Test
    void policyDecisionViewShowsRecentRedactedSummariesAndContextLinks() {
        AdminPolicyDecisionsView view = new AdminPolicyDecisionsView(new ConsoleHttpClient());
        view.showPolicyDecisions(sampleOverview().policyDecisions());

        assertThat(view.policyDecisionsPath()).isEqualTo("/api/admin/governance/policy-decisions");
        assertThat(view.renderedText())
                .contains("ALLOW")
                .contains("safe read-only tool")
                .contains("builtin.info")
                .contains("call-1")
                .contains("session-1")
                .contains("run-1")
                .contains("[REDACTED]")
                .doesNotContain("sk-test-secret")
                .doesNotContain("rawSecret");
        assertThat(view.contextLinks()).contains("/console/sessions/session-1", "/console/sessions/session-1/runs/run-1");
        assertThat(view.phaseFiveDeferredControlsPresent()).isFalse();
    }

    @Test
    void auditViewShowsRecentRedactedSummariesAndOmitsSearchFilterExport() {
        AdminAuditView view = new AdminAuditView(new ConsoleHttpClient());
        view.showAudits(sampleOverview().audits());

        assertThat(view.auditsPath()).isEqualTo("/api/admin/governance/audits");
        assertThat(view.renderedText())
                .contains("tool.policy")
                .contains("tool")
                .contains("builtin.info")
                .contains("session-1")
                .contains("run-1")
                .contains("[REDACTED]")
                .doesNotContain("sk-test-secret")
                .doesNotContain("apiKey")
                .doesNotContain("password");
        assertThat(view.contextLinks()).contains("/console/sessions/session-1", "/console/sessions/session-1/runs/run-1");
        assertThat(view.phaseFiveDeferredControlsPresent()).isFalse();
        assertThat(view.controlText().toLowerCase())
                .doesNotContain("search")
                .doesNotContain("filter")
                .doesNotContain("export");
    }

    static GovernanceOverviewResponse sampleOverview() {
        return new GovernanceOverviewResponse(
                new GovernanceStatusDto("runtime", "HEALTHY", "Runtime query API available", 1, Map.of("mode", "cloud")),
                new GovernanceStatusDto("providers", "HEALTHY", "Model provider registry available", 1, Map.of("defaultProvider", "openai-compatible")),
                new GovernanceStatusDto("toolRegistry", "HEALTHY", "Governed tool registry available", 2, Map.of("surface", "read-only")),
                new GovernanceStatusDto("extensions", "FUTURE_ENABLED", "SPI and Spring extension governance arrives in Phase 6", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                new GovernanceStatusDto("mcp", "UNCONFIGURED", "Remote MCP governance arrives in Phase 7", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                new GovernanceStatusDto("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                List.of(new PolicyDecisionSummaryDto("decision-1", "ALLOW", "safe read-only tool", "builtin.info", "call-1", "session-1", "run-1", Instant.parse("2026-06-15T00:00:00Z"), Map.of("summary", "[REDACTED]"))),
                List.of(new AuditSummaryDto("audit-1", "tool.policy", "tool", "builtin.info", "session-1", "run-1", Instant.parse("2026-06-15T00:00:01Z"), Map.of("decision", "ALLOW", "secret", "[REDACTED]"))),
                Instant.parse("2026-06-15T00:02:00Z"));
    }

    static ExtensionGovernanceResponse sampleExtensions() {
        return new ExtensionGovernanceResponse(List.of(new ExtensionSourceDto(
                "test-spring-extension",
                "Test Spring Extension",
                "1.0.0",
                "SPRING_BEAN",
                "USABLE",
                true,
                "COMPATIBLE",
                "UP",
                "",
                List.of(new ExtensionCapabilityDto(
                        "listener.audit",
                        "EVENT_LISTENER",
                        "USABLE",
                        "1.0.0",
                        "test-spring-extension",
                        true,
                        "COMPATIBLE",
                        "UP",
                        Map.of("extension.sourceKind", "SPRING_BEAN", "error", "[REDACTED]"))))));
    }
}
