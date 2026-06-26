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
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Recent inspect-only policy decision summaries for Admin Governance. */
@Route(value = "admin/governance/policy-decisions", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Policy Decisions")
public class AdminPolicyDecisionsView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();
    private final List<String> contextLinks = new ArrayList<>();

    public AdminPolicyDecisionsView() {
        this(new ConsoleHttpClient());
    }

    public AdminPolicyDecisionsView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-policy-decisions");
        getElement().setAttribute("data-route", "admin-policy-decisions");
        getElement().setAttribute("data-admin-surface", "inspect-only");
        getElement().setAttribute("data-deferred-controls", "search-filter-export");
        renderEmpty();
    }

    public String policyDecisionsPath() {
        return httpClient.adminPolicyDecisionsPath();
    }

    public void showPolicyDecisions(List<PolicyDecisionSummaryDto> decisions) {
        renderedLines.clear();
        contextLinks.clear();
        removeAll();
        add(new H2(getTranslation("admin.policy.title")));
        add(new Span(getTranslation("admin.policy.description")));
        if (decisions == null || decisions.isEmpty()) {
            Span empty = new Span(getTranslation("admin.policy.empty"));
            empty.getElement().setAttribute("data-state", "empty-policy-decisions");
            add(empty);
            return;
        }
        for (PolicyDecisionSummaryDto decision : decisions) {
            addDecision(decision);
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
        add(new H2(getTranslation("admin.policy.title")));
        Span empty = new Span(getTranslation("admin.policy.notLoaded"));
        empty.getElement().setAttribute("data-state", "empty-policy-decisions");
        add(empty);
    }

    private void addDecision(PolicyDecisionSummaryDto decision) {
        String sessionLink = sessionLink(decision.sessionId());
        String runLink = runLink(decision.sessionId(), decision.runId());
        maybeAddLink(sessionLink);
        maybeAddLink(runLink);
        String text = "Decision " + safe(decision.id())
                + " | " + safe(decision.decision())
                + " | reason=" + safe(decision.reason())
                + " | tool=" + safe(decision.toolId())
                + " | toolCall=" + safe(decision.toolCallId())
                + " | session=" + safe(decision.sessionId())
                + " | run=" + safe(decision.runId())
                + " | decidedAt=" + decision.decidedAt()
                + " | summary=" + redactedMap(decision.redactedSummary());
        renderedLines.add(text);

        Div summary = new Div(
                AdminMobileCardSupport.labelValue("decision", decision.decision()),
                AdminMobileCardSupport.labelValue("reason", decision.reason()),
                AdminMobileCardSupport.labelValue("tool", decision.toolId()),
                AdminMobileCardSupport.labelValue("toolCall", decision.toolCallId()),
                AdminMobileCardSupport.labelValue("session", decision.sessionId()),
                AdminMobileCardSupport.labelValue("run", decision.runId()),
                AdminMobileCardSupport.labelValue("decidedAt", String.valueOf(decision.decidedAt())),
                AdminMobileCardSupport.statusChip(decision.decision()));
        summary.addClassName("pi-admin-card-summary");

        Div actions = new Div();
        if (!sessionLink.isBlank()) {
            actions.add(new Anchor(sessionLink, getTranslation("admin.policy.link.session")));
        }
        if (!runLink.isBlank()) {
            actions.add(new Anchor(runLink, getTranslation("admin.policy.link.run")));
        }
        actions.addClassName("pi-admin-action-row");
        actions.getElement().setAttribute("data-admin-actions", "true");

        PiPageSection card = PiPageSection.card(
                "policy-decision-" + safe(decision.id()),
                new H3(getTranslation("admin.policy.cardTitle", safe(decision.id()))),
                summary,
                actions,
                redactedContextDetails(decision.redactedSummary()));
        card.addClassName("pi-admin-card");
        card.getElement().setAttribute("data-policy-decision-card", "true");
        card.getElement().setAttribute("data-policy-decision-id", safe(decision.id()));
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
    }

    private com.vaadin.flow.component.details.Details redactedContextDetails(Map<String, String> values) {
        Div fields = new Div();
        fields.addClassName("pi-admin-card-grid");
        if (values == null || values.isEmpty()) {
            fields.add(AdminMobileCardSupport.labelValue("context", "none"));
        } else {
            values.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> fields.add(AdminMobileCardSupport.labelValue(detailKey(entry.getKey()), detailValue(entry.getValue()))));
        }
        com.vaadin.flow.component.details.Details details = AdminMobileCardSupport.details(getTranslation("admin.policy.details"), "structured", fields);
        details.getElement().setAttribute("data-admin-details", "policy-context");
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
