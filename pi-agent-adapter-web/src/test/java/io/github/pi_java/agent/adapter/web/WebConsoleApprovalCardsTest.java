package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.ApprovalCommandService;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminApprovalQueueView;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalCard;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalDecisionHandler;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalPanel;
import io.github.pi_java.agent.adapter.web.ui.console.AppApprovalDecisionHandler;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
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
                .contains("Approval required")
                .contains("Risk")
                .contains("Side effect")
                .contains("Policy reason")
                .contains("Expected consequence")
                .contains("Provision preview")
                .contains("Arguments")
                .contains("Approve")
                .contains("Reject")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value");
        assertThat(card.getElement().getAttribute("data-event-category")).isEqualTo("approval");
        assertThat(card.getElement().getAttribute("class")).contains("pi-approval-card").contains("pi-card");
        assertThat(card.getElement().getAttribute("data-risk-level")).isEqualTo("MEDIUM");
        assertThat(card.getElement().getAttribute("data-side-effect")).isEqualTo("WORKSPACE_WRITE");
        assertThat(card.getElement().getAttribute("data-approval-id")).isEqualTo("preview-1");
        assertThat(card.getElement().getAttribute("data-tool-call-id")).isEqualTo("tool-call-1");
        assertThat(card.getElement().getAttribute("data-actor-role")).isEqualTo("USER");
        assertThat(card.detailsText())
                .contains("eligibleRoles=[USER]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value");
    }

    @Test
    void approvalCardRedactsRawSensitivePreviewAndArgumentValues() {
        ApprovalSummaryDto approval = new ApprovalSummaryDto("session-1", "run-1", "preview-1", "tool-call-1",
                "builtin.workspace.write", "builtin.workspace.write", "workspace writes require approval", "HIGH",
                "EXTERNAL_CALL", Map.of("apiKey", "sk-live-secret", "impact", "write token=raw-token-value"),
                Map.of("authorization", "bearer raw-token-value", "path", "notes/approval.txt"),
                "Approve resumes the gated tool path; reject records a same-run policy outcome.", true,
                Set.of("USER"));

        ApprovalCard card = new ApprovalCard(approval, new ConsoleHttpClient(), "USER");

        assertThat(card.summaryText())
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value")
                .doesNotContain("bearer raw-token-value");
        assertThat(card.detailsText())
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value")
                .doesNotContain("bearer raw-token-value");
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
        assertThat(reject.request().actorRole()).isEqualTo("USER");
        assertThat(card.statusFeedback()).contains("Reject requested").contains("same run timeline");
    }

    @Test
    void clickingApproveCallsDecisionHandlerAndShowsSuccessFeedback() {
        RecordingApprovalDecisionHandler handler = new RecordingApprovalDecisionHandler();
        ApprovalCard card = new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER", handler);

        button(card, "data-risk-action", "approve").click();

        assertThat(handler.plans).hasSize(1);
        ApprovalCard.DecisionPlan plan = handler.plans.getFirst();
        assertThat(plan.path()).isEqualTo("/api/sessions/session-1/runs/run-1/approvals/preview-1/decision");
        assertThat(plan.request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.APPROVE);
        assertThat(plan.request().actorRole()).isEqualTo("USER");
        assertThat(plan.sessionId()).isEqualTo("session-1");
        assertThat(plan.runId()).isEqualTo("run-1");
        assertThat(plan.approvalId()).isEqualTo("preview-1");
        assertThat(plan.toolCallId()).isEqualTo("tool-call-1");
        assertThat(card.statusFeedback()).contains("Decision recorded:").contains("APPROVED");
        assertThat(card.getElement().getAttribute("data-decision-state")).isEqualTo("succeeded");
        assertThat(button(card, "data-risk-action", "approve").isEnabled()).isFalse();
        assertThat(button(card, "data-risk-action", "reject").isEnabled()).isFalse();
        assertThat(flattenedElements(card))
                .anySatisfy(component -> {
                    assertThat(component.getElement().getAttribute("data-role")).isEqualTo("approval-decision-feedback");
                    assertThat(component.getElement().getText()).contains("Decision recorded:").contains("APPROVED");
                });
    }

    @Test
    void clickingRejectCallsDecisionHandlerAndShowsRejectedFeedback() {
        RecordingApprovalDecisionHandler handler = new RecordingApprovalDecisionHandler();
        ApprovalCard card = new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER", handler);

        button(card, "data-risk-action", "reject").click();

        assertThat(handler.plans).hasSize(1);
        ApprovalCard.DecisionPlan plan = handler.plans.getFirst();
        assertThat(plan.request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.REJECT);
        assertThat(plan.request().actorRole()).isEqualTo("USER");
        assertThat(plan.sessionId()).isEqualTo("session-1");
        assertThat(plan.runId()).isEqualTo("run-1");
        assertThat(plan.approvalId()).isEqualTo("preview-1");
        assertThat(plan.toolCallId()).isEqualTo("tool-call-1");
        assertThat(card.statusFeedback()).contains("Decision recorded:").contains("REJECTED");
        assertThat(card.getElement().getAttribute("data-decision-state")).isEqualTo("succeeded");
    }

    @Test
    void failedApprovalDecisionShowsRetryableFailureFeedback() {
        ApprovalCard card = new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER", plan -> {
            throw new RuntimeException("boom");
        });

        button(card, "data-risk-action", "approve").click();

        assertThat(card.statusFeedback()).isEqualTo("Decision failed: boom");
        assertThat(card.getElement().getAttribute("data-decision-state")).isEqualTo("failed");
        assertThat(button(card, "data-risk-action", "approve").isEnabled()).isTrue();
        assertThat(button(card, "data-risk-action", "reject").isEnabled()).isTrue();
    }

    @Test
    void appApprovalDecisionHandlerDelegatesWithRoleSpecificAuthorities() {
        List<RequestContext> contexts = new java.util.ArrayList<>();
        List<ApprovalCard.DecisionPlan> plans = new java.util.ArrayList<>();
        ApprovalCommandService service = (context, sessionId, runId, approvalId, request) -> {
            contexts.add(context);
            plans.add(new ApprovalCard.DecisionPlan("delegated", request, sessionId, runId, approvalId, "tool-call-1"));
            return response(new ApprovalCard.DecisionPlan("delegated", request, sessionId, runId, approvalId, "tool-call-1"));
        };
        AppApprovalDecisionHandler handler = new AppApprovalDecisionHandler(service);

        handler.decide(new ApprovalCard(approvalSummary("ADMIN"), new ConsoleHttpClient(), "ADMIN").planApprove("admin ok"));
        handler.decide(new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER").planReject("user no"));

        assertThat(plans).hasSize(2);
        assertThat(contexts.get(0).principal().authorities()).containsExactly("ROLE_ADMIN");
        assertThat(contexts.get(1).principal().authorities()).containsExactly("ROLE_USER");
        assertThat(contexts.get(0).principal().userId()).isEqualTo("vaadin-approval");
        assertThat(contexts.get(0).correlation().traceId()).isEqualTo("vaadin-approval");
    }

    @Test
    void inlineApprovalActionsHaveStableTouchSafeActionRowHooks() {
        ApprovalCard card = new ApprovalCard(approvalSummary("USER"), new ConsoleHttpClient(), "USER");

        assertThat(card.getChildren())
                .anySatisfy(child -> assertThat(child.getElement().getAttribute("data-approval-actions")).isEqualTo("inline"));
        assertThat(flattenedElements(card))
                .anySatisfy(component -> {
                    assertThat(component.getElement().getAttribute("data-action")).isEqualTo("approve-tool-call");
                    assertThat(component.getElement().getAttribute("data-risk-action")).isEqualTo("approve");
                })
                .anySatisfy(component -> {
                    assertThat(component.getElement().getAttribute("data-action")).isEqualTo("reject-tool-call");
                    assertThat(component.getElement().getAttribute("data-risk-action")).isEqualTo("reject");
                })
                .anySatisfy(component -> assertThat(component.getElement().getAttribute("data-role"))
                        .isEqualTo("approval-decision-feedback"));
    }

    @Test
    void adminDecisionPlansUseAdminActorRoleForApproveAndReject() {
        ApprovalCard card = new ApprovalCard(approvalSummary("ADMIN"), new ConsoleHttpClient(), "ADMIN");

        ApprovalCard.DecisionPlan approve = card.planApprove("admin approves");
        ApprovalCard.DecisionPlan reject = card.planReject("admin rejects");

        assertThat(approve.request().actorRole()).isEqualTo("ADMIN");
        assertThat(reject.request().actorRole()).isEqualTo("ADMIN");
        assertThat(approve.toolCallId()).isEqualTo("tool-call-1");
        assertThat(reject.toolCallId()).isEqualTo("tool-call-1");
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
    void rendererApprovalCardsUseSuppliedDecisionHandler() {
        RecordingApprovalDecisionHandler handler = new RecordingApprovalDecisionHandler();
        RunEventRenderer renderer = new RunEventRenderer(new ConsoleHttpClient(), handler);

        RunEventRenderer.RenderedEvent rendered = renderer.render(approvalRequiredEvent());
        button(rendered.component(), "data-risk-action", "approve").click();

        assertThat(handler.plans).hasSize(1);
        assertThat(handler.plans.getFirst().request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.APPROVE);
        assertThat(handler.plans.getFirst().request().actorRole()).isEqualTo("USER");
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
    void approvalPanelCardsUseSuppliedDecisionHandlerWithUserRole() {
        RecordingApprovalDecisionHandler handler = new RecordingApprovalDecisionHandler();
        ApprovalPanel panel = new ApprovalPanel(new ConsoleHttpClient(), "USER", handler);

        panel.showApprovals(List.of(approvalSummary("USER")));
        button(panel, "data-risk-action", "reject").click();

        assertThat(handler.plans).hasSize(1);
        assertThat(handler.plans.getFirst().request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.REJECT);
        assertThat(handler.plans.getFirst().request().actorRole()).isEqualTo("USER");
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

    @Test
    void adminApprovalQueueCardsUseSuppliedHandlerWithAdminRole() {
        RecordingApprovalDecisionHandler handler = new RecordingApprovalDecisionHandler();
        AdminApprovalQueueView view = new AdminApprovalQueueView(new ConsoleHttpClient(), handler);

        view.showPendingApprovals(List.of(approvalSummary("ADMIN")));
        button(view, "data-risk-action", "approve").click();

        assertThat(handler.plans).hasSize(1);
        assertThat(handler.plans.getFirst().request().decision()).isEqualTo(ApprovalDecisionRequest.Decision.APPROVE);
        assertThat(handler.plans.getFirst().request().actorRole()).isEqualTo("ADMIN");
    }

    private static List<Component> flattenedElements(Component component) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(component),
                        component.getChildren().flatMap(child -> flattenedElements(child).stream()))
                .toList();
    }

    private static Button button(Component component, String attribute, String value) {
        return flattenedElements(component).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(candidate -> value.equals(candidate.getElement().getAttribute(attribute)))
                .findFirst()
                .orElseThrow();
    }

    private static ApprovalDecisionResponse response(ApprovalCard.DecisionPlan plan) {
        return new ApprovalDecisionResponse(
                plan.sessionId(),
                plan.runId(),
                plan.approvalId(),
                plan.toolCallId(),
                plan.request().decision(),
                plan.request().decision() == ApprovalDecisionRequest.Decision.APPROVE ? "APPROVED" : "REJECTED",
                "test-principal",
                plan.request().actorRole(),
                plan.request().reason(),
                Instant.parse("2026-06-24T00:00:00Z"));
    }

    private static final class RecordingApprovalDecisionHandler implements ApprovalDecisionHandler {
        private final List<ApprovalCard.DecisionPlan> plans = new java.util.ArrayList<>();

        @Override
        public ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan) {
            plans.add(plan);
            return response(plan);
        }
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
