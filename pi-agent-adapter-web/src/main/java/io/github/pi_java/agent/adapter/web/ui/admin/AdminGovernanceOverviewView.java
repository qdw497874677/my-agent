package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Inspect-only Admin Governance landing page backed by public governance API DTOs. */
@Route(value = "admin/governance/overview", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Governance Overview")
public class AdminGovernanceOverviewView extends Div {

    private final ConsoleHttpClient httpClient;
    private GovernanceOverviewResponse overview;
    private final List<String> renderedLines = new ArrayList<>();

    public AdminGovernanceOverviewView() {
        this(new ConsoleHttpClient());
    }

    public AdminGovernanceOverviewView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-governance-overview");
        getElement().setAttribute("data-route", "admin-governance-overview");
        getElement().setAttribute("data-admin-surface", "inspect-only");
        renderEmpty();
    }

    public String overviewPath() {
        return httpClient.adminGovernanceOverviewPath();
    }

    public String registryStatusPath() {
        return "/admin/governance/registry";
    }

    public String operationsPath() {
        return "/admin/governance/operations";
    }

    public void showOverview(GovernanceOverviewResponse overview) {
        this.overview = Objects.requireNonNull(overview, "overview must not be null");
        renderedLines.clear();
        removeAll();
        add(new H2(getTranslation("admin.overview.title")));
        add(new Span(getTranslation("admin.overview.description")));
        addStatus(getTranslation("admin.overview.runtime"), overview.runtime());
        addStatus(getTranslation("admin.overview.modelProviders"), overview.providers());
        addStatus(getTranslation("admin.overview.toolRegistry"), overview.toolRegistry());
        addStatus(getTranslation("admin.overview.extensions"), overview.extensions());
        addStatus(getTranslation("admin.overview.mcp"), overview.mcp());
        addStatus(getTranslation("admin.overview.plugins"), overview.plugins());
        addOperationsLink();
        addMetric(getTranslation("admin.overview.policyDecisions"), overview.policyDecisions().size(), "/admin/governance/policy-decisions");
        addMetric(getTranslation("admin.overview.auditSummaries"), overview.audits().size(), "/admin/governance/audits");
        add(new Span(getTranslation("admin.overview.generatedAt", overview.generatedAt())));
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    public GovernanceOverviewResponse overview() {
        return overview;
    }

    private void renderEmpty() {
        renderedLines.clear();
        removeAll();
        add(new H2(getTranslation("admin.overview.title")));
        Span empty = new Span(getTranslation("admin.overview.empty"));
        empty.getElement().setAttribute("data-state", "empty-governance-overview");
        add(empty);
    }

    private void addStatus(String label, GovernanceStatusDto status) {
        String text = label + ": " + status.status()
                + " | count=" + status.count()
                + " | " + status.message()
                + " | " + metadata(status.metadata());
        renderedLines.add(text);
        Div actions = AdminMobileCardSupport.actionRow(
                AdminMobileCardSupport.actionLink(registryStatusPath(), getTranslation("admin.overview.link.registry")),
                AdminMobileCardSupport.actionLink(operationsPath(), getTranslation("admin.overview.link.operations")),
                AdminMobileCardSupport.actionLink("/admin/governance/policy-decisions", getTranslation("admin.overview.link.policyDecisions")),
                AdminMobileCardSupport.actionLink("/admin/governance/audits", getTranslation("admin.overview.link.auditSummaries")));
        Div card = AdminMobileCardSupport.statusCard(
                status.area(),
                label,
                status.status(),
                String.valueOf(status.count()),
                status.message(),
                actions,
                AdminMobileCardSupport.metadataDetails(status.metadata()));
        card.getElement().setAttribute("data-admin-overview-card", status.area());
        card.getElement().setAttribute("data-governance-area", status.area());
        card.getElement().setAttribute("data-governance-status", status.status());
        add(card);
    }

    private void addMetric(String label, int count, String href) {
        String text = label + ": " + count;
        renderedLines.add(text);
        add(new Anchor(href, text));
    }

    private void addOperationsLink() {
        renderedLines.add(getTranslation("admin.overview.link.operationsMetrics"));
        add(new Anchor(operationsPath(), getTranslation("admin.overview.link.operationsMetrics")));
    }

    private static String metadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "metadata=none";
        }
        return metadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> safe(entry.getKey()) + "=" + safe(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
