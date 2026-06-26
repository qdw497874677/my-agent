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

    private static final List<String> ABNORMAL_TOKENS = List.of(
            "ERROR", "FAILED", "DOWN", "WARN", "WARNING", "UNHEALTHY");

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
        addMetricSection(getTranslation("admin.ops.runs"), response.runs());
        addMetricSection(getTranslation("admin.ops.models"), response.models());
        addMetricSection(getTranslation("admin.ops.tools"), response.tools());
        addMetricSection(getTranslation("admin.ops.policies"), response.policies());
        addMetricSection(getTranslation("admin.ops.mcp"), response.mcp());
        addMetricSection(getTranslation("admin.ops.plugins"), response.plugins());
        addMetricSection(getTranslation("admin.ops.errors"), response.errors());
        addWarningSection(getTranslation("admin.ops.warnings"), response.warnings());
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
        add(new H2(getTranslation("admin.ops.title")));
        Span empty = new Span(getTranslation("admin.ops.empty"));
        empty.getElement().setAttribute("data-state", "empty-operations-summary");
        add(empty);
        addMetricSection(getTranslation("admin.ops.runs"), List.of());
        addMetricSection(getTranslation("admin.ops.models"), List.of());
        addMetricSection(getTranslation("admin.ops.tools"), List.of());
        addMetricSection(getTranslation("admin.ops.policies"), List.of());
        addMetricSection(getTranslation("admin.ops.mcp"), List.of());
        addMetricSection(getTranslation("admin.ops.plugins"), List.of());
        addMetricSection(getTranslation("admin.ops.errors"), List.of());
        addWarningSection(getTranslation("admin.ops.warnings"), List.of());
    }

    private void addHeader(OperationsSummaryResponse response) {
        renderedLines.add(getTranslation("admin.ops.title"));
        renderedLines.add("generatedAt=" + response.generatedAt());
        add(new H2(getTranslation("admin.ops.title")));
        add(new Span(getTranslation("admin.ops.description")));
        add(new Span(getTranslation("admin.ops.generatedAt", response.generatedAt())));
    }

    private void addMetricSection(String label, List<OperationMetricDto> metrics) {
        renderedLines.add(label);
        Div section = new Div(new H3(label));
        section.getElement().setAttribute("data-operations-section", label.toLowerCase().replace(' ', '-'));
        if (metrics.isEmpty()) {
            String emptyText = getTranslation("admin.ops.noneReported", label);
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
                section.add(metricCard(label, metric));
            });
        }
        add(section);
    }

    private void addWarningSection(String label, List<OperationalWarningDto> warnings) {
        renderedLines.add(label);
        Div section = new Div(new H3(label));
        section.getElement().setAttribute("data-operations-section", "warnings");
        if (warnings.isEmpty()) {
            String emptyText = getTranslation("admin.ops.warningsNone");
            renderedLines.add(emptyText);
            section.add(new Span(emptyText));
        } else {
            warnings.forEach(warning -> {
                String text = "Warnings: " + warning.area()
                        + " | severity=" + safe(warning.severity())
                        + " | message=" + safe(warning.message())
                        + " | " + metadata(warning.metadata());
                renderedLines.add(text);
                section.add(warningCard(warning));
            });
        }
        add(section);
    }

    private Div metricCard(String sectionLabel, OperationMetricDto metric) {
        String area = safe(metric.area());
        String status = safe(metric.status());
        String severity = metricSeverity(sectionLabel, metric);
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("area", area),
                AdminMobileCardSupport.labelValue("name", safe(metric.name())),
                AdminMobileCardSupport.labelValue("status", status),
                AdminMobileCardSupport.labelValue("value", String.valueOf(metric.value())),
                AdminMobileCardSupport.labelValue("unit", safe(metric.unit())),
                severityChip(status, severity));
        summary.addClassName("pi-admin-card-summary");
        Div card = AdminMobileCardSupport.metricCard(
                normalizeSection(sectionLabel),
                safe(metric.name()),
                String.valueOf(metric.value()),
                area + " " + status,
                summary,
                AdminMobileCardSupport.metadataDetails(metric.metadata()));
        card.getElement().setAttribute("data-operations-card", normalizeSection(sectionLabel));
        card.getElement().setAttribute("data-operations-area", area);
        card.getElement().setAttribute("data-operations-status", status);
        card.getElement().setAttribute("data-status-severity", severity);
        return card;
    }

    private Div warningCard(OperationalWarningDto warning) {
        String area = safe(warning.area());
        String severity = warningSeverity(warning.severity());
        String status = safe(warning.severity());
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("severity", status),
                AdminMobileCardSupport.labelValue("area", area),
                AdminMobileCardSupport.labelValue("message", safe(warning.message())),
                severityChip(status, severity));
        summary.addClassName("pi-admin-card-summary");
        Div card = AdminMobileCardSupport.metricCard(
                "warnings",
                area + " warning",
                status,
                safe(warning.message()),
                summary,
                AdminMobileCardSupport.metadataDetails(warning.metadata()));
        card.getElement().setAttribute("data-operations-warning-card", area);
        card.getElement().setAttribute("data-operations-area", area);
        card.getElement().setAttribute("data-operations-status", status);
        card.getElement().setAttribute("data-status-severity", severity);
        return card;
    }

    private Span severityChip(String label, String severity) {
        Span chip = AdminMobileCardSupport.statusChip(label);
        chip.getElement().setAttribute("data-status-severity", severity);
        return chip;
    }

    private static String metricSeverity(String sectionLabel, OperationMetricDto metric) {
        if (containsAbnormalToken(metric.status())
                || containsAbnormalToken(metric.name())
                || ("errors".equals(normalizeSection(sectionLabel)) && metric.value() != 0.0d)) {
            return "abnormal";
        }
        return "normal";
    }

    private static String warningSeverity(String value) {
        return containsAbnormalToken(value) ? "abnormal" : "normal";
    }

    private static boolean containsAbnormalToken(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        return ABNORMAL_TOKENS.stream().anyMatch(normalized::contains);
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

    private static String normalizeSection(String value) {
        return safe(value).toLowerCase().replace(' ', '-');
    }
}
