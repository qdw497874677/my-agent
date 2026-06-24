package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import java.util.Objects;

/** User/Admin approval decision card for gated governed tool calls. */
public class ApprovalCard extends Div {

    private final ApprovalSummaryDto approval;
    private final ConsoleHttpClient httpClient;
    private final String actorRole;
    private final String summaryText;
    private String statusFeedback = "Awaiting backend approval decision for the original run/tool call.";

    public ApprovalCard(ApprovalSummaryDto approval, ConsoleHttpClient httpClient, String actorRole) {
        this.approval = Objects.requireNonNull(approval, "approval must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.actorRole = normalizeRole(actorRole);
        this.summaryText = buildSummary(approval);
        addClassNames("pi-approval-card", "pi-card");
        getElement().setAttribute("data-event-category", "approval");
        getElement().setAttribute("data-session-id", safe(approval.sessionId()));
        getElement().setAttribute("data-run-id", safe(approval.runId()));
        getElement().setAttribute("data-approval-id", safe(approval.approvalId()));
        getElement().setAttribute("data-tool-call-id", safe(approval.toolCallId()));
        getElement().setAttribute("data-actor-role", this.actorRole);
        getElement().setAttribute("data-risk-level", fallback(approval.riskLabel(), "unknown"));
        getElement().setAttribute("data-side-effect", fallback(approval.sideEffectLabel(), "unknown"));

        Div header = new Div();
        header.addClassName("pi-approval-card__header");
        header.add(new H4("Approval required"), new Span(fallback(approval.toolName(), approval.toolId())));

        Div riskGrid = new Div();
        riskGrid.addClassName("pi-approval-card__risk-grid");
        riskGrid.add(
                field("Risk", fallback(approval.riskLabel(), "unknown")),
                field("Side effect", fallback(approval.sideEffectLabel(), "unknown")),
                field("Policy reason", fallback(approval.policyReason(), "review required")),
                field("Expected consequence", fallback(approval.expectedConsequence(), "Approve or reject changes the original run state.")));

        Div preview = new Div();
        preview.addClassName("pi-approval-card__preview");
        preview.add(
                field("Provision preview", RuntimeDetailRedactor.stringify(approval.provisionPreview())),
                field("Arguments", RuntimeDetailRedactor.stringify(approval.redactedArgumentSummary())));

        Button approve = new Button("Approve");
        approve.addClassName("pi-approval-card__approve");
        approve.getElement().setAttribute("theme", "primary success");
        approve.getElement().setAttribute("data-action", "approve-tool-call");
        approve.getElement().setAttribute("data-risk-action", "approve");
        Button reject = new Button("Reject");
        reject.addClassName("pi-approval-card__reject");
        reject.getElement().setAttribute("theme", "error tertiary");
        reject.getElement().setAttribute("data-action", "reject-tool-call");
        reject.getElement().setAttribute("data-risk-action", "reject");
        Div actionRow = new Div();
        actionRow.addClassName("pi-action-row");
        actionRow.getElement().setAttribute("data-approval-actions", "inline");
        actionRow.add(approve, reject);
        Span feedback = new Span(statusFeedback);
        feedback.addClassName("pi-approval-card__feedback");
        feedback.getElement().setAttribute("data-role", "approval-decision-feedback");
        add(header, riskGrid, preview, new Span("Actions: Approve/Reject"), new Details("Decision context", new Span(detailsText())), actionRow, feedback);
    }

    public static ApprovalCard from(ApprovalSummaryDto approval, ConsoleHttpClient httpClient) {
        return new ApprovalCard(approval, httpClient, "USER");
    }

    public DecisionPlan planApprove(String reason) {
        return plan(ApprovalDecisionRequest.Decision.APPROVE, reason);
    }

    public DecisionPlan planReject(String reason) {
        return plan(ApprovalDecisionRequest.Decision.REJECT, reason);
    }

    public String summaryText() {
        return summaryText;
    }

    public String detailsText() {
        return "session=" + safe(approval.sessionId())
                + " | run=" + safe(approval.runId())
                + " | approval=" + safe(approval.approvalId())
                + " | toolCall=" + safe(approval.toolCallId())
                + " | preview=" + RuntimeDetailRedactor.stringify(approval.provisionPreview())
                + " | arguments=" + RuntimeDetailRedactor.stringify(approval.redactedArgumentSummary())
                + " | eligibleRoles=" + RuntimeDetailRedactor.stringify(approval.eligibleActorRoles());
    }

    public String statusFeedback() {
        return statusFeedback;
    }

    public ApprovalSummaryDto approval() {
        return approval;
    }

    private DecisionPlan plan(ApprovalDecisionRequest.Decision decision, String reason) {
        ApprovalDecisionRequest request = new ApprovalDecisionRequest(decision, reason, actorRole);
        statusFeedback = decision == ApprovalDecisionRequest.Decision.APPROVE
                ? "Approve requested through backend approval API; waiting for same run timeline update."
                : "Reject requested through backend approval API; waiting for same run timeline update.";
        return new DecisionPlan(
                httpClient.approvalDecisionPath(approval.sessionId(), approval.runId(), approval.approvalId()),
                request,
                approval.sessionId(),
                approval.runId(),
                approval.approvalId(),
                approval.toolCallId());
    }

    private static String buildSummary(ApprovalSummaryDto approval) {
        return String.join(" | ",
                "Approval required for " + fallback(approval.toolName(), approval.toolId()),
                "Policy reason=" + fallback(approval.policyReason(), "review required"),
                "Risk=" + fallback(approval.riskLabel(), "unknown"),
                "Side effect=" + fallback(approval.sideEffectLabel(), "unknown"),
                "Provision preview=" + RuntimeDetailRedactor.stringify(approval.provisionPreview()),
                "Arguments=" + RuntimeDetailRedactor.stringify(approval.redactedArgumentSummary()),
                "Expected consequence=" + fallback(approval.expectedConsequence(), "Approve or reject changes the original run state."),
                "Actions=Approve/Reject");
    }

    private static Div field(String label, String value) {
        Div field = new Div();
        field.addClassName("pi-approval-card__field");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("pi-approval-card__label");
        Span valueSpan = new Span(fallback(value, "unknown"));
        valueSpan.addClassName("pi-approval-card__value");
        field.add(labelSpan, valueSpan);
        return field;
    }

    private static String normalizeRole(String role) {
        return role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.trim();
        String lower = sanitized.toLowerCase();
        if (lower.contains("sk-live-")
                || lower.contains("raw-token-value")
                || lower.contains("api_key=")
                || lower.contains("api-key=")
                || lower.contains("password=")
                || lower.contains("secret=")
                || lower.contains("token=")) {
            return "[REDACTED]";
        }
        return sanitized;
    }

    public record DecisionPlan(
            String path,
            ApprovalDecisionRequest request,
            String sessionId,
            String runId,
            String approvalId,
            String toolCallId) {
    }
}
