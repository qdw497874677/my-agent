package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminApprovalQueueView;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalCard;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebConsoleApprovalCardsTest {

    @Test
    void approvalCardShowsSafeDecisionContextAndActions() {
        ApprovalSummaryDto approval = approvalSummary("USER");

        ApprovalCard card = new ApprovalCard(approval, new ConsoleHttpClient(), "USER");

        assertThat(card.summaryText())
                .contains("builtin.workspace.write")
                .contains("workspace writes require approval")
                .contains("MEDIUM")
                .contains("WORKSPACE_WRITE")
                .contains("write notes/approval.txt")
                .contains("path=notes/approval.txt")
                .contains("Approve resumes")
                .contains("Approve")
                .contains("Reject")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value");
        assertThat(card.getElement().getAttribute("data-event-category")).isEqualTo("approval");
        assertThat(card.getElement().getAttribute("data-approval-id")).isEqualTo("preview-1");
        assertThat(card.getElement().getAttribute("data-tool-call-id")).isEqualTo("tool-call-1");
    }

    @Test
    void approveAndRejectDecisionPlansUseOriginalSessionRunAndApprovalIds() {
        ApprovalCard card = new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER");

        ApprovalCard.DecisionPlan approve = card.planApprove("looks safe");
        ApprovalCard.DecisionPlan reject = card.planReject("not safe");

        assertThat(approve.path()).isEqualTo("/api/sessions/session-1/runs/run-1/approvals/preview-1/decision");
        assertThat(approve.request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.APPROVE);
        assertThat(approve.request().actorRole()).isEqualTo("USER");
        assertThat(reject.path()).isEqualTo("/api/sessions/session-1/runs/run-1/approvals/preview-1/decision");
        assertThat(reject.request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.REJECT);
        assertThat(card.statusFeedback()).contains("Reject requested").contains("same run timeline");
    }

    @Test
    void rendererTurnsApprovalRequiredLifecycleEventsIntoApprovalCards() {
        RunEventRenderer renderer = new RunEventRenderer(new ConsoleHttpClient());

        RunEventRenderer.RenderedEvent rendered = renderer.render(approvalRequiredEvent());

        assertThat(rendered.category()).isEqualTo("approval");
        assertThat(rendered.component()).isInstanceOf(ApprovalCard.class);
        assertThat(rendered.text()).contains("builtin.workspace.write").contains("Approve resumes");
    }

    @Test
    void chatPanelAppendsApprovalComponentAndNarrativeStatus() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();
        RunEventRenderer renderer = new RunEventRenderer(new ConsoleHttpClient());

        panel.appendEvent(renderer.render(approvalRequiredEvent()));

        assertThat(panel.messageCount()).isEqualTo(1);
        assertThat(panel.messages().getFirst()).contains("builtin.workspace.write");
        assertThat(panel.componentCount()).isEqualTo(1);
    }

    @Test
    void approvalPanelRendersPendingApprovalListFromPublicApiReadModel() {
        ApprovalPanel panel = new ApprovalPanel(new ConsoleHttpClient(), "USER");

        panel.showApprovals(List.of(approvalSummary("USER")));

        assertThat(panel.approvalsPath("session-1", "run-1"))
                .isEqualTo("/api/sessions/session-1/runs/run-1/approvals");
        assertThat(panel.cardCount()).isEqualTo(1);
        assertThat(panel.renderedText()).contains("builtin.workspace.write").contains("workspace writes require approval");
    }

    @Test
    void adminApprovalQueueListsPendingApprovalsAndUsesAdminDecisionRole() {
        AdminApprovalQueueView view = new AdminApprovalQueueView(new ConsoleHttpClient());

        view.showPendingApprovals(List.of(approvalSummary("ADMIN")));
        ApprovalCard.DecisionPlan plan = view.planApprove("preview-1", "admin approved");

        assertThat(view.getElement().getAttribute("data-route")).isEqualTo("admin-approval-queue");
        assertThat(view.getElement().getAttribute("data-admin-surface")).isEqualTo("separated-governance");
        assertThat(view.pendingCount()).isEqualTo(1);
        assertThat(view.renderedText())
                .contains("session-1")
                .contains("run-1")
                .contains("tool-call-1")
                .contains("builtin.workspace.write");
        assertThat(plan.path()).isEqualTo("/api/sessions/session-1/runs/run-1/approvals/preview-1/decision");
        assertThat(plan.request().actorRole()).isEqualTo("ADMIN");
        assertThat(plan.request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.APPROVE);
    }

    private static ApprovalSummaryDto approvalSummary(String role) {
        return new ApprovalSummaryDto("session-1", "run-1", "preview-1", "tool-call-1",
                "builtin.workspace.write", "builtin.workspace.write", "workspace writes require approval", "MEDIUM",
                "WORKSPACE_WRITE", Map.of("impact", "write notes/approval.txt", "approvalRecommended", true),
                Map.of("path", "notes/approval.txt", "content", "[REDACTED]"),
                "Approve resumes the gated tool path; reject records a same-run policy outcome.", true,
                Set.of(role));
    }

    private static RunEventDto approvalRequiredEvent() {
        return new RunEventDto(
                "event-approval-1",
                "tenant-1",
                "user-1",
                "session-1",
                "run-1",
                "step-1",
                "workspace-1",
                4,
                Instant.parse("2026-06-15T00:00:00Z"),
                "tool.approval_required",
                "trace-1",
                "correlation-1",
                "cause-1",
                "USER",
                null,
                "tool.lifecycle",
                1,
                Map.ofEntries(
                        Map.entry("toolCallId", "tool-call-1"),
                        Map.entry("toolId", "builtin.workspace.write"),
                        Map.entry("toolName", "builtin.workspace.write"),
                        Map.entry("status", "APPROVAL_REQUIRED"),
                        Map.entry("policyReason", "workspace writes require approval"),
                        Map.entry("riskLevel", "MEDIUM"),
                        Map.entry("sideEffect", "WORKSPACE_WRITE"),
                        Map.entry("previewId", "preview-1"),
                        Map.entry("preview", Map.of("impact", "write notes/approval.txt", "approvalRecommended", true)),
                        Map.entry("argumentSummary", Map.of("path", "notes/approval.txt", "content", "[REDACTED]")),
                        Map.entry("expectedConsequence", "Approve resumes the gated tool path; reject records a same-run policy outcome.")));
    }
}
