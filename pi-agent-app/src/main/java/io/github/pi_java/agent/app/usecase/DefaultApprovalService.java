package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
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
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class DefaultApprovalService implements ApprovalQueryService, ApprovalCommandService {

    private static final Set<String> ELIGIBLE_ROLES = Set.of("USER", "ADMIN");

    private final RunEventStore runEventStore;
    private final AuditRepository auditRepository;
    private final EventSink eventSink;
    private final Clock clock;
    private final Supplier<String> eventIdSupplier;

    public DefaultApprovalService(RunEventStore runEventStore, AuditRepository auditRepository, EventSink eventSink,
                                  Clock clock) {
        this(runEventStore, auditRepository, eventSink, clock, () -> UUID.randomUUID().toString());
    }

    public DefaultApprovalService(RunEventStore runEventStore, AuditRepository auditRepository, EventSink eventSink,
                                  Clock clock, Supplier<String> eventIdSupplier) {
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventIdSupplier = Objects.requireNonNull(eventIdSupplier, "eventIdSupplier must not be null");
    }

    @Override
    public List<ApprovalSummaryDto> listPendingApprovals(RequestContext context, String sessionId, String runId) {
        String actorRole = actorRole(context, "USER");
        return runEventStore.listByRun(runId, 0, 1_000).stream()
                .filter(event -> event.type() == RunEventType.TOOL_APPROVAL_REQUIRED)
                .filter(event -> sessionId.equals(event.sessionId().value()))
                .filter(event -> runId.equals(event.runId().value()))
                .map(event -> summary(event, actorRole))
                .toList();
    }

    @Override
    public ApprovalDecisionResponse decide(RequestContext context, String sessionId, String runId, String approvalId,
                                           ApprovalDecisionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String actorRole = actorRole(context, request.actorRole());
        if (!ELIGIBLE_ROLES.contains(actorRole)) {
            throw new IllegalArgumentException("actor role is not eligible to decide approvals: " + actorRole);
        }
        RunEvent approvalEvent = runEventStore.listByRun(runId, 0, 1_000).stream()
                .filter(event -> event.type() == RunEventType.TOOL_APPROVAL_REQUIRED)
                .filter(event -> sessionId.equals(event.sessionId().value()))
                .filter(event -> approvalId.equals(approvalId(event)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        Instant decidedAt = clock.instant();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("decision", request.decision().name());
        details.put("actorPrincipal", context.userId());
        details.put("actorRole", actorRole);
        details.put("approvalId", approvalId);
        details.put("toolCallId", toolCallId(approvalEvent));
        details.put("reason", request.reason());
        auditRepository.record(context, "approval." + request.decision().name().toLowerCase(), "approval", approvalId,
                sessionId, runId, details);
        publishDecisionEvent(context, approvalEvent, request, actorRole, decidedAt, details);
        if (request.decision() == ApprovalDecisionRequest.Decision.REJECT) {
            publishRejectedOutcome(context, approvalEvent, request, actorRole, decidedAt, approvalId);
        }
        return new ApprovalDecisionResponse(sessionId, runId, approvalId, toolCallId(approvalEvent), request.decision(),
                request.decision() == ApprovalDecisionRequest.Decision.APPROVE ? "APPROVED" : "REJECTED",
                context.userId(), actorRole, request.reason(), decidedAt);
    }

    private ApprovalSummaryDto summary(RunEvent event, String actorRole) {
        RunEventPayload.ToolLifecyclePayload payload = (RunEventPayload.ToolLifecyclePayload) event.payload();
        Map<String, Object> preview = payload.preview().map(this::preview).orElse(Map.of());
        String policyReason = payload.errorCategory().orElse("approval_required");
        String sideEffect = String.valueOf(payload.provenance().metadata().getOrDefault("sideEffect", "APPROVAL_REQUIRED"));
        String risk = String.valueOf(payload.provenance().metadata().getOrDefault("risk", "REVIEW_REQUIRED"));
        return new ApprovalSummaryDto(event.sessionId().value(), event.runId().value(), approvalId(event),
                payload.toolCallId(), payload.toolId(), payload.toolId(), policyReason, risk, sideEffect, preview,
                payload.redactedInputSummary(), "Approve resumes the gated tool path; reject records a same-run policy outcome.",
                ELIGIBLE_ROLES.contains(actorRole), ELIGIBLE_ROLES);
    }

    private void publishDecisionEvent(RequestContext context, RunEvent original, ApprovalDecisionRequest request,
                                      String actorRole, Instant decidedAt, Map<String, Object> details) {
        publishExtension(context, original, RunEventType.POLICY_DECIDED, "approval.decision", decidedAt, details);
    }

    private void publishRejectedOutcome(RequestContext context, RunEvent original, ApprovalDecisionRequest request,
                                        String actorRole, Instant decidedAt, String approvalId) {
        Map<String, Object> denied = new LinkedHashMap<>();
        denied.put("status", "POLICY_BLOCKED");
        denied.put("approvalId", approvalId);
        denied.put("decision", request.decision().name());
        denied.put("actorPrincipal", context.userId());
        denied.put("actorRole", actorRole);
        denied.put("reason", request.reason());
        publishExtension(context, original, RunEventType.RUN_POLICY_BLOCKED, "approval.rejected", decidedAt, denied);
    }

    private void publishExtension(RequestContext context, RunEvent original, RunEventType type, String schema,
                                  Instant timestamp, Map<String, Object> attributes) {
        long sequence = runEventStore.findLastByRun(original.runId().value()).map(RunEvent::sequence).orElse(0L) + 1;
        eventSink.publish(new RunEvent(eventIdSupplier.get(), new TenantId(context.tenantId()), new UserId(context.userId()),
                new SessionId(original.sessionId().value()), new RunId(original.runId().value()),
                new StepId(original.stepId().value()), new WorkspaceId(original.workspaceId().value()), sequence, timestamp, type,
                new TraceId(context.traceId()), new CorrelationId(context.correlationId()), new CausationId(context.causationId()),
                new RunEventPayload.ExtensionPayload(schema, "1", attributes), EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "approval-service")));
    }

    private Map<String, Object> preview(io.github.pi_java.agent.domain.tool.ProvisionPreview preview) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("previewId", preview.previewId());
        values.put("summary", preview.summary());
        values.put("impacts", preview.impacts());
        values.put("approvalRecommended", preview.approvalRecommended());
        values.put("redactedDetails", preview.redactedDetails());
        return Map.copyOf(values);
    }

    private static String approvalId(RunEvent event) {
        return ((RunEventPayload.ToolLifecyclePayload) event.payload()).preview()
                .map(io.github.pi_java.agent.domain.tool.ProvisionPreview::previewId)
                .orElse(((RunEventPayload.ToolLifecyclePayload) event.payload()).toolCallId());
    }

    private static String toolCallId(RunEvent event) {
        return ((RunEventPayload.ToolLifecyclePayload) event.payload()).toolCallId();
    }

    private static String actorRole(RequestContext context, String requestedRole) {
        if (requestedRole != null && !requestedRole.isBlank()) {
            return requestedRole.toUpperCase();
        }
        return context.principal().authorities().stream().anyMatch(value -> value.equals("ROLE_ADMIN") || value.equals("ADMIN"))
                ? "ADMIN" : "USER";
    }
}
