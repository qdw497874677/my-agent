package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminGovernanceOverviewView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminOperationsView;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.OperationMetricDto;
import io.github.pi_java.agent.client.admin.OperationalWarningDto;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminOperationsViewTest {

    @Test
    void consoleHttpClientExposesOperationsSummaryApiAnchor() {
        ConsoleHttpClient client = new ConsoleHttpClient();

        assertThat(client.adminGovernanceOperationsPath()).isEqualTo("/api/admin/governance/operations");
        assertThat(client.adminGovernanceOperationsResponseType()).isEqualTo(OperationsSummaryResponse.class);
    }

    @Test
    void overviewLinksToOperationsMetricsDetails() {
        AdminGovernanceOverviewView view = new AdminGovernanceOverviewView(new ConsoleHttpClient());
        view.showOverview(sampleOverview());

        assertThat(view.operationsPath()).isEqualTo("/admin/governance/operations");
        assertThat(view.renderedText()).contains("Operations metrics");
    }

    @Test
    void operationsViewRendersSummarySectionsFromPublicDtoOnly() {
        AdminOperationsView view = new AdminOperationsView(new ConsoleHttpClient());
        view.showOperations(sampleOperations());

        assertThat(view.operationsPath()).isEqualTo("/api/admin/governance/operations");
        assertThat(view.getElement().getAttribute("data-route")).isEqualTo("admin-operations");
        assertThat(view.getElement().getAttribute("data-admin-surface")).isEqualTo("operations-summary");
        assertThat(view.renderedText())
                .contains("Operations metrics")
                .contains("Runs")
                .contains("Models")
                .contains("Tools")
                .contains("Policies")
                .contains("MCP")
                .contains("Plugins")
                .contains("Errors")
                .contains("Warnings")
                .contains("pi.run.events.total")
                .contains("policy decisions healthy")
                .contains("generatedAt=2026-06-19T00:00:00Z")
                .doesNotContain("PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK");
    }

    @Test
    void operationsViewDoesNotExposeExplorerOrMutationControls() {
        AdminOperationsView view = new AdminOperationsView(new ConsoleHttpClient());
        view.showOperations(sampleOperations());

        assertThat(view.renderedText().toLowerCase())
                .doesNotContain("export")
                .doesNotContain("query builder")
                .doesNotContain("chart editor")
                .doesNotContain("time-range analytics")
                .doesNotContain("delete")
                .doesNotContain("disable");
        assertThat(view.explorerControlsPresent()).isFalse();
    }

    private static GovernanceOverviewResponse sampleOverview() {
        GovernanceStatusDto status = new GovernanceStatusDto("runtime", "HEALTHY", "ready", 1, Map.of());
        return new GovernanceOverviewResponse(status, status, status, status, status, status, List.of(), List.of(), Instant.parse("2026-06-19T00:00:00Z"));
    }

    private static OperationsSummaryResponse sampleOperations() {
        return new OperationsSummaryResponse(
                List.of(metric("runs", "pi.run.events.total", "HEALTHY", 12.0, "count")),
                List.of(metric("models", "pi.model.calls.total", "HEALTHY", 4.0, "count")),
                List.of(metric("tools", "pi.tool.calls.total", "HEALTHY", 3.0, "count")),
                List.of(metric("policies", "policy decisions healthy", "HEALTHY", 2.0, "count")),
                List.of(metric("mcp", "pi.mcp.calls.total", "HEALTHY", 1.0, "count")),
                List.of(metric("plugins", "pi.plugin.calls.total", "HEALTHY", 1.0, "count")),
                List.of(metric("errors", "pi.errors.total", "HEALTHY", 0.0, "count")),
                List.of(new OperationalWarningDto("warnings", "INFO", "No external observability backend required", Map.of("safe", "true"))),
                Instant.parse("2026-06-19T00:00:00Z"));
    }

    private static OperationMetricDto metric(String area, String name, String status, double value, String unit) {
        return new OperationMetricDto(area, name, status, value, unit, Map.of("source", "public-dto"));
    }
}
