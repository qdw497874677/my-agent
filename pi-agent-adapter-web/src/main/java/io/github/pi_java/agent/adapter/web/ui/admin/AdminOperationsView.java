package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.client.admin.OperationMetricDto;
import io.github.pi_java.agent.client.admin.OperationalWarningDto;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only operations summary backed by the public Admin Governance operations DTO. */
@Route(value = "admin/governance/operations", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Operations Metrics")
public class AdminOperationsView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();
    private OperationsSummaryResponse operations;

    public AdminOperationsView() {
        this(new ConsoleHttpClient());
    }

    public AdminOperationsView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-operations-summary");
        getElement().setAttribute("data-route", "admin-operations");
        getElement().setAttribute("data-admin-surface", "operations-summary");
        renderEmpty();
    }

    public String operationsPath() {
        return httpClient.adminGovernanceOperationsPath();
    }

    public void showOperations(OperationsSummaryResponse response) {
        this.operations = Objects.requireNonNull(response, "response must not be null");
        renderedLines.clear();
        removeAll();
        addHeader(response);
        addMetricSection("Runs", response.runs());
        addMetricSection("Models", response.models());
        addMetricSection("Tools", response.tools());
        addMetricSection("Policies", response.policies());
        addMetricSection("MCP", response.mcp());
        addMetricSection("Plugins", response.plugins());
        addMetricSection("Errors", response.errors());
        addWarningSection("Warnings", response.warnings());
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    public OperationsSummaryResponse operations() {
        return operations;
    }

    public boolean explorerControlsPresent() {
        return false;
    }

    private void renderEmpty() {
        renderedLines.clear();
        removeAll();
        add(new H2("Operations metrics"));
        Span empty = new Span("Operations summary has not been loaded.");
        empty.getElement().setAttribute("data-state", "empty-operations-summary");
        add(empty);
        addMetricSection("Runs", List.of());
        addMetricSection("Models", List.of());
        addMetricSection("Tools", List.of());
        addMetricSection("Policies", List.of());
        addMetricSection("MCP", List.of());
        addMetricSection("Plugins", List.of());
        addMetricSection("Errors", List.of());
        addWarningSection("Warnings", List.of());
    }

    private void addHeader(OperationsSummaryResponse response) {
        renderedLines.add("Operations metrics");
        renderedLines.add("generatedAt=" + response.generatedAt());
        add(new H2("Operations metrics"));
        add(new Span("Read-only production operations summary from public Admin Governance APIs."));
        add(new Span("Generated at: " + response.generatedAt()));
    }

    private void addMetricSection(String label, List<OperationMetricDto> metrics) {
        renderedLines.add(label);
        Div section = new Div(new H3(label));
        section.getElement().setAttribute("data-operations-section", label.toLowerCase().replace(' ', '-'));
        if (metrics.isEmpty()) {
            String emptyText = label + ": none reported";
            renderedLines.add(emptyText);
            section.add(new Span(emptyText));
        } else {
            metrics.forEach(metric -> {
                String text = label + ": " + metric.area()
                        + " | name=" + safe(metric.name())
                        + " | status=" + safe(metric.status())
                        + " | value=" + metric.value()
                        + " " + safe(metric.unit())
                        + " | " + metadata(metric.metadata());
                renderedLines.add(text);
                section.add(new Span(text));
            });
        }
        add(section);
    }

    private void addWarningSection(String label, List<OperationalWarningDto> warnings) {
        renderedLines.add(label);
        Div section = new Div(new H3(label));
        section.getElement().setAttribute("data-operations-section", "warnings");
        if (warnings.isEmpty()) {
            String emptyText = "Warnings: none reported";
            renderedLines.add(emptyText);
            section.add(new Span(emptyText));
        } else {
            warnings.forEach(warning -> {
                String text = "Warnings: " + warning.area()
                        + " | severity=" + safe(warning.severity())
                        + " | message=" + safe(warning.message())
                        + " | " + metadata(warning.metadata());
                renderedLines.add(text);
                section.add(new Span(text));
            });
        }
        add(section);
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
