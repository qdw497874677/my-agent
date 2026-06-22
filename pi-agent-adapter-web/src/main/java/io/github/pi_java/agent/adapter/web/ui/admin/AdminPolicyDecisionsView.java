package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
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
        add(new H2("Recent Policy Decisions"));
        add(new Span("Redacted recent policy decisions. Full search, filtering, and export are deferred."));
        if (decisions == null || decisions.isEmpty()) {
            Span empty = new Span("No recent policy decisions.");
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
        add(new H2("Recent Policy Decisions"));
        Span empty = new Span("Policy decisions have not been loaded.");
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
        Div row = new Div(new Span(text));
        row.getElement().setAttribute("data-policy-decision-id", decision.id());
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
