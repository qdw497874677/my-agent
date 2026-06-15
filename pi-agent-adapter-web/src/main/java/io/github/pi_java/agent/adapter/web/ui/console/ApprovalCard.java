package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        addClassName("pi-approval-card");
        getElement().setAttribute("data-event-category", "approval");
        getElement().setAttribute("data-session-id", safe(approval.sessionId()));
        getElement().setAttribute("data-run-id", safe(approval.runId()));
        getElement().setAttribute("data-approval-id", safe(approval.approvalId()));
        getElement().setAttribute("data-tool-call-id", safe(approval.toolCallId()));
        getElement().setAttribute("data-actor-role", this.actorRole);
        Button approve = new Button("Approve");
        approve.getElement().setAttribute("data-action", "approve-tool-call");
        Button reject = new Button("Reject");
        reject.getElement().setAttribute("data-action", "reject-tool-call");
        add(new Span(summaryText), new Details("Decision context", new Span(detailsText())), approve, reject);
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
                + " | preview=" + value(approval.provisionPreview())
                + " | arguments=" + value(approval.redactedArgumentSummary())
                + " | eligibleRoles=" + value(approval.eligibleActorRoles());
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
                "policy=" + fallback(approval.policyReason(), "review required"),
                "risk=" + fallback(approval.riskLabel(), "unknown"),
                "sideEffect=" + fallback(approval.sideEffectLabel(), "unknown"),
                "preview=" + value(approval.provisionPreview()),
                "arguments=" + value(approval.redactedArgumentSummary()),
                "expected=" + fallback(approval.expectedConsequence(), "Approve or reject changes the original run state."),
                "actions=Approve/Reject");
    }

    private static String normalizeRole(String role) {
        return role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> safe(String.valueOf(entry.getKey())) + "=" + value(entry.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(ApprovalCard::value).collect(Collectors.joining(", ", "[", "]"));
        }
        return safe(String.valueOf(value));
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
