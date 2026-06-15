package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalCard;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Admin Governance approval inspection and decision surface. */
@Route("admin/governance/approvals")
@PageTitle("Pi Admin Approval Queue")
public class AdminApprovalQueueView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<ApprovalCard> cards = new ArrayList<>();

    public AdminApprovalQueueView() {
        this(new ConsoleHttpClient());
    }

    public AdminApprovalQueueView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-approval-queue");
        getElement().setAttribute("data-route", "admin-approval-queue");
        getElement().setAttribute("data-admin-surface", "separated-governance");
        add(new H2("Approval Queue"));
        showPendingApprovals(List.of());
    }

    public void showPendingApprovals(List<ApprovalSummaryDto> approvals) {
        cards.clear();
        removeAll();
        add(new H2("Approval Queue"));
        if (approvals == null || approvals.isEmpty()) {
            Span empty = new Span("No pending approvals.");
            empty.getElement().setAttribute("data-state", "empty-admin-approvals");
            add(empty);
            return;
        }
        for (ApprovalSummaryDto approval : approvals) {
            Div context = new Div("Session " + approval.sessionId()
                    + " | Run " + approval.runId()
                    + " | Tool call " + approval.toolCallId());
            context.getElement().setAttribute("data-approval-context", approval.approvalId());
            ApprovalCard card = new ApprovalCard(approval, httpClient, "ADMIN");
            cards.add(card);
            add(context, card);
        }
    }

    public ApprovalCard.DecisionPlan planApprove(String approvalId, String reason) {
        return find(approvalId).planApprove(reason);
    }

    public ApprovalCard.DecisionPlan planReject(String approvalId, String reason) {
        return find(approvalId).planReject(reason);
    }

    public int pendingCount() {
        return cards.size();
    }

    public String renderedText() {
        return cards.stream()
                .map(card -> "session=" + card.approval().sessionId()
                        + " | run=" + card.approval().runId()
                        + " | toolCall=" + card.approval().toolCallId()
                        + " | " + card.summaryText())
                .collect(Collectors.joining("\n"));
    }

    private ApprovalCard find(String approvalId) {
        return cards.stream()
                .filter(card -> card.approval().approvalId().equals(approvalId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown approval: " + approvalId));
    }
}
