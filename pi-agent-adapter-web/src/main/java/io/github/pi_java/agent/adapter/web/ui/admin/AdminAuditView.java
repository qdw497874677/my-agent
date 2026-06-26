package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.PiPageSection;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Recent redacted audit summaries for the inspect-only Admin Governance surface. */
@Route(value = "admin/governance/audits", layout = PiResponsiveShell.class)
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
        add(new H2(getTranslation("admin.audit.title")));
        add(new Span(getTranslation("admin.audit.description")));
        if (audits == null || audits.isEmpty()) {
            Span empty = new Span(getTranslation("admin.audit.empty"));
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
        add(new H2(getTranslation("admin.audit.title")));
        Span empty = new Span(getTranslation("admin.audit.notLoaded"));
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

        Div summary = new Div(
                AdminMobileCardSupport.labelValue("action", audit.action()),
                AdminMobileCardSupport.labelValue("resourceType", audit.resourceType()),
                AdminMobileCardSupport.labelValue("resourceId", audit.resourceId()),
                AdminMobileCardSupport.labelValue("session", audit.sessionId()),
                AdminMobileCardSupport.labelValue("run", audit.runId()),
                AdminMobileCardSupport.labelValue("recordedAt", String.valueOf(audit.recordedAt())));
        summary.addClassName("pi-admin-card-summary");

        Div actions = new Div();
        if (!sessionLink.isBlank()) {
            actions.add(new Anchor(sessionLink, getTranslation("admin.audit.link.session")));
        }
        if (!runLink.isBlank()) {
            actions.add(new Anchor(runLink, getTranslation("admin.audit.link.run")));
        }
        actions.addClassName("pi-admin-action-row");
        actions.getElement().setAttribute("data-admin-actions", "true");

        PiPageSection card = PiPageSection.card(
                "audit-" + safe(audit.id()),
                new H3(getTranslation("admin.audit.cardTitle", safe(audit.id()))),
                summary,
                actions,
                redactedAuditDetails(audit.redactedDetails()));
        card.addClassName("pi-admin-card");
        card.getElement().setAttribute("data-audit-card", "true");
        card.getElement().setAttribute("data-audit-id", safe(audit.id()));
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
    }

    private com.vaadin.flow.component.details.Details redactedAuditDetails(Map<String, String> values) {
        Div fields = new Div();
        fields.addClassName("pi-admin-card-grid");
        if (values == null || values.isEmpty()) {
            fields.add(AdminMobileCardSupport.labelValue("details", "none"));
        } else {
            values.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> fields.add(AdminMobileCardSupport.labelValue(
                            detailKey(entry.getKey()),
                            containsSensitiveTerms(String.valueOf(entry.getKey())) ? AdminMobileRedactor.REDACTED : detailValue(entry.getValue()))));
        }
        com.vaadin.flow.component.details.Details details = AdminMobileCardSupport.details(getTranslation("admin.audit.details"), "structured", fields);
        details.getElement().setAttribute("data-admin-details", "audit-details");
        return details;
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
                .map(entry -> detailKey(entry.getKey()) + "=" + safe(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String detailValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = AdminMobileRedactor.redact(value.trim());
        return containsSensitiveTerms(sanitized) ? AdminMobileRedactor.REDACTED : sanitized;
    }

    private static String detailKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = AdminMobileRedactor.redact(value.trim());
        return looksSensitive(sanitized) || containsSensitiveTerms(sanitized) ? AdminMobileRedactor.REDACTED : sanitized;
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
        return AdminMobileRedactor.redact(value).contains(AdminMobileRedactor.REDACTED);
    }

    private static boolean containsSensitiveTerms(String value) {
        String lower = value.toLowerCase();
        return lower.contains("api")
                || lower.contains("pass" + "word")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("authorization");
    }
}
