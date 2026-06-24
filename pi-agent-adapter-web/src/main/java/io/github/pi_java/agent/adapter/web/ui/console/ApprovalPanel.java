package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Container for pending approval cards loaded from the public approval API. */
public class ApprovalPanel extends Div {

    private final ConsoleHttpClient httpClient;
    private final String actorRole;
    private final ApprovalDecisionHandler approvalDecisionHandler;
    private final List<ApprovalCard> cards = new ArrayList<>();

    public ApprovalPanel() {
        this(new ConsoleHttpClient(), "USER");
    }

    public ApprovalPanel(ConsoleHttpClient httpClient, String actorRole) {
        this(httpClient, actorRole, ApprovalDecisionHandler.demo());
    }

    public ApprovalPanel(ConsoleHttpClient httpClient, String actorRole, ApprovalDecisionHandler approvalDecisionHandler) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.actorRole = actorRole == null || actorRole.isBlank() ? "USER" : actorRole.trim().toUpperCase();
        this.approvalDecisionHandler = Objects.requireNonNull(approvalDecisionHandler, "approvalDecisionHandler must not be null");
        addClassName("pi-approval-panel");
        getElement().setAttribute("data-panel", "approvals");
        add(new H3("Pending approvals"));
        showApprovals(List.of());
    }

    public void showApprovals(List<ApprovalSummaryDto> approvals) {
        cards.clear();
        removeAll();
        add(new H3("Pending approvals"));
        if (approvals == null || approvals.isEmpty()) {
            Span empty = new Span("No pending approvals for this run.");
            empty.getElement().setAttribute("data-state", "empty-approvals");
            add(empty);
            return;
        }
        for (ApprovalSummaryDto approval : approvals) {
            ApprovalCard card = new ApprovalCard(approval, httpClient, actorRole, approvalDecisionHandler);
            cards.add(card);
            add(card);
        }
    }

    public String approvalsPath(String sessionId, String runId) {
        return httpClient.approvalsPath(sessionId, runId);
    }

    public int cardCount() {
        return cards.size();
    }

    public List<ApprovalCard> cards() {
        return List.copyOf(cards);
    }

    public String renderedText() {
        return cards.stream().map(ApprovalCard::summaryText).collect(Collectors.joining("\n"));
    }
}
