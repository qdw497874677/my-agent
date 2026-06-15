package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Recent redacted audit summaries for the inspect-only Admin Governance surface. */
@Route("admin/governance/audits")
@PageTitle("Pi Admin Audit Summaries")
public class AdminAuditView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();
    private final List<String> contextLinks = new ArrayList<>();

    public AdminAuditView() {
        this(new ConsoleHttpClient());
    }

    public AdminAuditView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-audit-summaries");
        getElement().setAttribute("data-route", "admin-audit-summaries");
        getElement().setAttribute("data-admin-surface", "inspect-only");
        getElement().setAttribute("data-deferred-controls", "search-filter-export");
        renderEmpty();
    }

    public String auditsPath() {
        return httpClient.adminAuditsPath();
    }

    public void showAudits(List<AuditSummaryDto> audits) {
        renderedLines.clear();
        contextLinks.clear();
        removeAll();
        add(new H2("Recent Audit Summaries"));
        add(new Span("Redacted recent audit summaries. Full search, filtering, and export are deferred."));
        if (audits == null || audits.isEmpty()) {
            Span empty = new Span("No recent audit summaries.");
            empty.getElement().setAttribute("data-state", "empty-audit-summaries");
            add(empty);
            return;
        }
        for (AuditSummaryDto audit : audits) {
            addAudit(audit);
        }
    }

    public boolean phaseFiveDeferredControlsPresent() {
        return false;
    }

    public String controlText() {
        return "";
    }

    public List<String> contextLinks() {
        return List.copyOf(contextLinks);
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    private void renderEmpty() {
        renderedLines.clear();
        contextLinks.clear();
        removeAll();
        add(new H2("Recent Audit Summaries"));
        Span empty = new Span("Audit summaries have not been loaded.");
        empty.getElement().setAttribute("data-state", "empty-audit-summaries");
        add(empty);
    }

    private void addAudit(AuditSummaryDto audit) {
        String sessionLink = sessionLink(audit.sessionId());
        String runLink = runLink(audit.sessionId(), audit.runId());
        maybeAddLink(sessionLink);
        maybeAddLink(runLink);
        String text = "Audit " + safe(audit.id())
                + " | action=" + safe(audit.action())
                + " | resourceType=" + safe(audit.resourceType())
                + " | resourceId=" + safe(audit.resourceId())
                + " | session=" + safe(audit.sessionId())
                + " | run=" + safe(audit.runId())
                + " | recordedAt=" + audit.recordedAt()
                + " | details=" + redactedMap(audit.redactedDetails());
        renderedLines.add(text);
        Div row = new Div(new Span(text));
        row.getElement().setAttribute("data-audit-id", audit.id());
        row.getElement().setAttribute("data-read-only", "true");
        if (!sessionLink.isBlank()) {
            row.add(new Anchor(sessionLink, "Session"));
        }
        if (!runLink.isBlank()) {
            row.add(new Anchor(runLink, "Run"));
        }
        add(row);
    }

    private void maybeAddLink(String link) {
        if (!link.isBlank()) {
            contextLinks.add(link);
        }
    }

    private static String sessionLink(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "" : "/console/sessions/" + safe(sessionId);
    }

    private static String runLink(String sessionId, String runId) {
        if (sessionId == null || sessionId.isBlank() || runId == null || runId.isBlank()) {
            return "";
        }
        return "/console/sessions/" + safe(sessionId) + "/runs/" + safe(runId);
    }

    private static String redactedMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> safe(entry.getKey()) + "=" + safe(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value.trim();
        if (looksSensitive(sanitized)) {
            return "[REDACTED]";
        }
        return sanitized;
    }

    private static boolean looksSensitive(String value) {
        String lower = value.toLowerCase();
        return lower.contains("sk-") || lower.contains("rawsecret") || lower.contains("apikey") || lower.contains("password") || lower.contains("token=");
    }
}
