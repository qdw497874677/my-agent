package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only provider/tool/extension/MCP/plugin registry status view for Admin Governance. */
@Route("admin/governance/registry")
@PageTitle("Pi Admin Registry Status")
public class AdminRegistryStatusView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();

    public AdminRegistryStatusView() {
        this(new ConsoleHttpClient());
    }

    public AdminRegistryStatusView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-registry-status");
        getElement().setAttribute("data-route", "admin-registry-status");
        getElement().setAttribute("data-admin-surface", "inspect-only");
        getElement().setAttribute("data-mutation-controls", "absent");
        renderEmpty();
    }

    public String overviewPath() {
        return httpClient.adminGovernanceOverviewPath();
    }

    public void showOverview(GovernanceOverviewResponse overview) {
        Objects.requireNonNull(overview, "overview must not be null");
        renderedLines.clear();
        removeAll();
        add(new H2("Registry and Integration Status"));
        add(new Span("Provider, tool, extension, MCP, and plugin status is inspect-only in Phase 5."));
        addStatus("Model Providers", overview.providers());
        addStatus("Tool Registry", overview.toolRegistry());
        addStatus("Extensions", overview.extensions());
        addStatus("MCP", overview.mcp());
        addStatus("Plugins", overview.plugins());
    }

    public boolean mutationControlsPresent() {
        return false;
    }

    public String mutationActionText() {
        return "";
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    private void renderEmpty() {
        renderedLines.clear();
        removeAll();
        add(new H2("Registry and Integration Status"));
        Span empty = new Span("Registry status has not been loaded.");
        empty.getElement().setAttribute("data-state", "empty-registry-status");
        add(empty);
    }

    private void addStatus(String label, GovernanceStatusDto status) {
        String text = label + ": " + status.status()
                + " | count=" + status.count()
                + " | " + status.message()
                + " | " + metadata(status.metadata());
        renderedLines.add(text);
        Div card = new Div(new H3(label), new Span(text));
        card.getElement().setAttribute("data-governance-area", status.area());
        card.getElement().setAttribute("data-governance-status", status.status());
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
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
