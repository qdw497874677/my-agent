package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.usecase.DefaultApprovalService;
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApprovalControllerTest {

    @Test
    void approvalSummariesExposeSafeDecisionContextAndActorEligibility() {
        Harness harness = new Harness();
        harness.store.append(approvalRequiredEvent());

        List<ApprovalSummaryDto> approvals = harness.service.listPendingApprovals(context("ROLE_USER"), "session-1", "run-1");

        assertThat(approvals).hasSize(1);
        ApprovalSummaryDto approval = approvals.getFirst();
        assertThat(approval.toolName()).isEqualTo("builtin.workspace.write");
        assertThat(approval.policyReason()).isEqualTo("approval_required");
        assertThat(approval.riskLabel()).isEqualTo("REVIEW_REQUIRED");
        assertThat(approval.sideEffectLabel()).isEqualTo("APPROVAL_REQUIRED");
        assertThat(approval.provisionPreview().toString()).contains("write notes/approval.txt", "approvalRecommended");
        assertThat(approval.redactedArgumentSummary()).containsEntry("path", "notes/approval.txt");
        assertThat(approval.expectedConsequence()).contains("Approve resumes");
        assertThat(approval.actorEligible()).isTrue();
        assertThat(approval.eligibleActorRoles()).contains("USER", "ADMIN");
    }

    @Test
    void approvalDecisionsRecordActorAndOriginalRunToolContext() {
        Harness harness = new Harness();
        harness.store.append(approvalRequiredEvent());

        ApprovalDecisionResponse response = harness.service.decide(context("ROLE_ADMIN"), "session-1", "run-1", "preview-1",
                new ApprovalDecisionRequest(ApprovalDecisionRequest.Decision.REJECT, "unsafe write", "ADMIN"));

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.runId()).isEqualTo("run-1");
        assertThat(response.approvalId()).isEqualTo("preview-1");
        assertThat(response.toolCallId()).isEqualTo("tool-call-1");
        assertThat(response.decision()).isEqualTo(ApprovalDecisionRequest.Decision.REJECT);
        assertThat(response.actorPrincipal()).isEqualTo("user-1");
        assertThat(response.actorRole()).isEqualTo("ADMIN");
        assertThat(harness.audits).anySatisfy(audit -> assertThat(audit.toString()).contains("approval.reject", "preview-1", "tool-call-1"));
        assertThat(harness.published).extracting(RunEvent::type).contains(RunEventType.POLICY_DECIDED, RunEventType.RUN_POLICY_BLOCKED);
    }

    private static RequestContext context(String authority) {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of(authority)),
                new CorrelationContext("trace-1", "corr-1", "cause-1"));
    }

    private static RunEvent approvalRequiredEvent() {
        return new RunEvent("event-approval-1", new TenantId("tenant-1"), new UserId("user-1"),
                new SessionId("session-1"), new RunId("run-1"), new StepId("step-1"), new WorkspaceId("workspace-1"),
                4, Instant.parse("2026-06-15T00:00:00Z"), RunEventType.TOOL_APPROVAL_REQUIRED,
                new TraceId("trace-1"), new CorrelationId("corr-1"), new CausationId("cause-1"),
                new RunEventPayload.ToolLifecyclePayload("tool-call-1", "builtin.workspace.write", "1",
                        new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin", "binding", Map.of()),
                        Map.of("path", "notes/approval.txt", "content", "[REDACTED]"), Map.of(),
                        PolicyDecision.REQUIRE_APPROVAL, ToolExecutionStatus.APPROVAL_REQUIRED,
                        new ProvisionPreview("preview-1", "write notes/approval.txt", Set.of("workspace write"), true,
                                Map.of("summary", "redacted write")), "approval_required"),
                EventVisibility.USER, new RedactionMetadata(true, true, Set.of("content"), "test"));
    }

    private static final class Harness {
        private final TestRunEventStore store = new TestRunEventStore();
        private final List<Map<String, Object>> audits = new ArrayList<>();
        private final List<RunEvent> published = new ArrayList<>();
        private final DefaultApprovalService service = new DefaultApprovalService(store,
                (context, action, resourceType, resourceId, sessionId, runId, details) -> audits.add(Map.of(
                        "action", action, "resourceId", resourceId, "sessionId", sessionId, "runId", runId, "details", details)),
                published::add, Clock.fixed(Instant.parse("2026-06-15T00:01:00Z"), ZoneOffset.UTC), () -> "decision-event");
    }

    private static final class TestRunEventStore implements RunEventStore {
        private final List<RunEvent> events = new ArrayList<>();

        @Override
        public void append(RunEvent event) {
            events.add(event);
        }

        @Override
        public List<RunEvent> listByRun(String runId, long afterSequence, int limit) {
            return events.stream().filter(event -> event.runId().value().equals(runId))
                    .filter(event -> event.sequence() > afterSequence).sorted(Comparator.comparingLong(RunEvent::sequence))
                    .limit(limit).toList();
        }

        @Override
        public Optional<RunEvent> findLastByRun(String runId) {
            return events.stream().filter(event -> event.runId().value().equals(runId)).max(Comparator.comparingLong(RunEvent::sequence));
        }

        @Override
        public boolean hasTerminalEvent(String runId) {
            return false;
        }
    }
}
