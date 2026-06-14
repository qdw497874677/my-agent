package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator.ValidationResult;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter;
import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter.LimitCheck;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluation;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluationRequest;
import io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator;
import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.app.port.tool.ToolRedactor.RedactedToolPayload;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload.ToolLifecyclePayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultToolExecutionGateway implements ToolExecutionGateway {

    private final ToolRegistry toolRegistry;
    private final ToolArgumentValidator argumentValidator;
    private final ToolPolicyEvaluator policyEvaluator;
    private final ToolRedactor redactor;
    private final ToolPayloadLimiter payloadLimiter;
    private final ToolPreviewGenerator previewGenerator;
    private final AuditRepository auditRepository;
    private final EventSink eventSink;
    private final Clock clock;
    private final AtomicLong eventSequence = new AtomicLong();

    public DefaultToolExecutionGateway(
            ToolRegistry toolRegistry,
            ToolArgumentValidator argumentValidator,
            ToolPolicyEvaluator policyEvaluator,
            ToolRedactor redactor,
            ToolPayloadLimiter payloadLimiter,
            ToolPreviewGenerator previewGenerator,
            AuditRepository auditRepository,
            EventSink eventSink,
            Clock clock
    ) {
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        this.argumentValidator = Objects.requireNonNull(argumentValidator, "argumentValidator must not be null");
        this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator must not be null");
        this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
        this.payloadLimiter = Objects.requireNonNull(payloadLimiter, "payloadLimiter must not be null");
        this.previewGenerator = Objects.requireNonNull(previewGenerator, "previewGenerator must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ToolExecutionRequest request = command.request();
        Instant startedAt = clock.instant();
        ToolRegistry.ToolResolution resolution = toolRegistry.resolve(request.toolId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + request.toolId()));
        ToolDescriptor descriptor = resolution.descriptor();
        RedactedToolPayload redactedArguments = redactor.redact(descriptor, request.arguments());

        publish(command, descriptor, RunEventType.TOOL_PROPOSED, redactedArguments.summary(), Map.of(), null, null, null, null,
                redactedArguments.redactedFields(), "default");
        audit(command, descriptor, "tool.proposed", details(redactedArguments.summary(), Map.of(), null, null, null));

        LimitCheck argumentLimit = payloadLimiter.checkArguments(descriptor, request.arguments());
        if (!argumentLimit.allowed()) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.FAILED, "argument payload limit exceeded",
                    "payload_limit", redactedArguments, Map.of(), null, RunEventType.TOOL_FAILED, null, "tool.validation_failed");
        }

        ValidationResult validation = argumentValidator.validate(descriptor, request);
        if (!validation.valid()) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.FAILED, validation.message(),
                    validation.errorCode().orElse("invalid_arguments"), redactedArguments, Map.of(), null,
                    RunEventType.TOOL_FAILED, null, "tool.validation_failed");
        }

        PolicyEvaluation policy = policyEvaluator.evaluate(new PolicyEvaluationRequest(command.context(), command.sessionId(),
                command.workspaceId(), descriptor, request, redactedArguments.summary()));
        publish(command, descriptor, RunEventType.TOOL_POLICY_DECIDED, redactedArguments.summary(), Map.of(), policy.decision(),
                null, null, null, redactedArguments.redactedFields(), policy.policyRef());
        audit(command, descriptor, "tool.policy_decided", details(redactedArguments.summary(), Map.of(), policy, null, null));

        ProvisionPreview preview = null;
        if (requiresPreview(descriptor, policy)) {
            preview = previewGenerator.generate(new ToolPreviewGenerator.PreviewRequest(command.context(), command.sessionId(),
                    command.workspaceId(), descriptor, request, policy, redactedArguments.summary()));
            publish(command, descriptor, RunEventType.TOOL_PREVIEW_GENERATED, redactedArguments.summary(), Map.of(), policy.decision(),
                    null, preview, null, redactedArguments.redactedFields(), policy.policyRef());
            audit(command, descriptor, "tool.preview_generated", details(redactedArguments.summary(), Map.of(), policy, preview, null));
        }

        if (policy.decision() == PolicyDecision.DENY || policy.decision() == PolicyDecision.BLOCK) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.DENIED, policy.reason(), policy.decision().name(),
                    redactedArguments, Map.of(), preview, RunEventType.TOOL_DENIED, policy, "tool.denied");
        }
        if (policy.decision() == PolicyDecision.REQUIRE_APPROVAL) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.APPROVAL_REQUIRED, policy.reason(),
                    "approval_required", redactedArguments, Map.of(), preview, RunEventType.TOOL_APPROVAL_REQUIRED, policy,
                    "tool.approval_required");
        }
        if (policy.decision() == PolicyDecision.REQUIRE_SANDBOX) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.SANDBOX_REQUIRED, policy.reason(),
                    "sandbox_required", redactedArguments, Map.of(), preview, RunEventType.TOOL_DENIED, policy,
                    "tool.sandbox_required");
        }
        if (command.cancellationToken().isCancellationRequested()) {
            return terminal(command, descriptor, startedAt, ToolExecutionStatus.CANCELLED,
                    command.cancellationToken().reason().orElse("cancelled"), "cancelled", redactedArguments, Map.of(), preview,
                    RunEventType.TOOL_CANCELLED, policy, "tool.cancelled");
        }

        publish(command, descriptor, RunEventType.TOOL_STARTED, redactedArguments.summary(), Map.of(), policy.decision(), null,
                preview, null, redactedArguments.redactedFields(), policy.policyRef());
        audit(command, descriptor, "tool.started", details(redactedArguments.summary(), Map.of(), policy, preview, null));

        ToolExecutionResult executed;
        try {
            executed = resolution.executor().execute(request, command.cancellationToken());
        } catch (RuntimeException ex) {
            ToolExecutionStatus status = isTimeout(ex) ? ToolExecutionStatus.TIMED_OUT : ToolExecutionStatus.FAILED;
            String category = isTimeout(ex) ? "timeout" : "execution_failed";
            return terminal(command, descriptor, startedAt, status, redactSummary(ex.getMessage()), category, redactedArguments,
                    Map.of(), preview, RunEventType.TOOL_FAILED, policy, "tool.failed");
        }

            Map<String, Object> rawOutput = executed.rawOutput().orElse(executed.redactedOutputSummary());
            LimitCheck resultLimit = payloadLimiter.checkResult(descriptor, rawOutput);
            Map<String, Object> summarizedOutput = payloadLimiter.summarize(descriptor, rawOutput);
            RedactedToolPayload redactedOutput = redactor.redact(descriptor, summarizedOutput);
            ToolExecutionStatus status = resultLimit.allowed() ? executed.status() : ToolExecutionStatus.FAILED;
            String errorCategory = resultLimit.allowed() ? executed.errorCategory().orElse(null) : "payload_limit";
            String summary = redactSummary(executed.summary());
            RunEventType terminalType = terminalEventType(status);
            ToolExecutionResult result = new ToolExecutionResult(request.toolCallId(), request.toolId(), status, summary,
                    Optional.ofNullable(errorCategory), redactedArguments.summary(), redactedOutput.summary(),
                    union(redactedArguments.redactedFields(), redactedOutput.redactedFields()), Optional.ofNullable(preview),
                    elapsed(startedAt), Optional.empty());
            publish(command, descriptor, terminalType, result.redactedInputSummary(), result.redactedOutputSummary(),
                    policy.decision(), status, preview, result.errorCategory().orElse(null), result.redactedFields(), policy.policyRef());
            audit(command, descriptor, auditAction(status), details(result.redactedInputSummary(), result.redactedOutputSummary(),
                    policy, preview, result.errorCategory().orElse(null)));
            return result;
    }

    private boolean requiresPreview(ToolDescriptor descriptor, PolicyEvaluation policy) {
        return policy.previewRequired() || descriptor.sideEffect() == io.github.pi_java.agent.domain.tool.ToolSideEffect.WORKSPACE_WRITE
                || descriptor.sideEffect() == io.github.pi_java.agent.domain.tool.ToolSideEffect.EXTERNAL_WRITE
                || descriptor.sideEffect() == io.github.pi_java.agent.domain.tool.ToolSideEffect.DESTRUCTIVE;
    }

    private ToolExecutionResult terminal(ToolExecutionCommand command, ToolDescriptor descriptor, Instant startedAt,
                                         ToolExecutionStatus status, String summary, String errorCategory,
                                         RedactedToolPayload redactedInput, Map<String, Object> redactedOutput,
                                         ProvisionPreview preview, RunEventType eventType, PolicyEvaluation policy,
                                         String auditAction) {
        ToolExecutionResult result = new ToolExecutionResult(command.request().toolCallId(), command.request().toolId(), status,
                redactSummary(summary), errorCategory, redactedInput.summary(), redactedOutput, redactedInput.redactedFields(),
                preview, elapsed(startedAt));
        publish(command, descriptor, eventType, result.redactedInputSummary(), result.redactedOutputSummary(),
                policy == null ? null : policy.decision(), status, preview, result.errorCategory().orElse(null),
                result.redactedFields(), policy == null ? "default" : policy.policyRef());
        audit(command, descriptor, auditAction, details(result.redactedInputSummary(), result.redactedOutputSummary(), policy,
                preview, result.errorCategory().orElse(null)));
        return result;
    }

    private void publish(ToolExecutionCommand command, ToolDescriptor descriptor, RunEventType type,
                         Map<String, Object> redactedInput, Map<String, Object> redactedOutput,
                         PolicyDecision policyDecision, ToolExecutionStatus status, ProvisionPreview preview,
                         String errorCategory, Set<String> redactedFields, String policyRef) {
        RequestContext context = command.context();
        eventSink.publish(new RunEvent(
                command.request().toolCallId() + "-" + type.wireName() + "-" + eventSequence.incrementAndGet(),
                new io.github.pi_java.agent.domain.common.PlatformIds.TenantId(context.tenantId()),
                new io.github.pi_java.agent.domain.common.PlatformIds.UserId(context.userId()),
                command.sessionId(),
                command.request().runId(),
                command.request().stepId(),
                command.workspaceId(),
                eventSequence.get(),
                clock.instant(),
                type,
                new TraceId(context.traceId()),
                new CorrelationId(context.correlationId()),
                new CausationId(context.causationId()),
                new ToolLifecyclePayload(command.request().toolCallId(), descriptor.id(), descriptor.version(),
                        descriptor.provenance(), redactedInput, redactedOutput, policyDecision, status, preview, errorCategory),
                EventVisibility.USER,
                new RedactionMetadata(!redactedFields.isEmpty(), !redactedFields.isEmpty(), redactedFields, policyRef)));
    }

    private void audit(ToolExecutionCommand command, ToolDescriptor descriptor, String action, Map<String, Object> details) {
        auditRepository.record(command.context(), action, "tool", descriptor.id(), command.sessionId().value(),
                command.request().runId().value(), details);
    }

    private Map<String, Object> details(Map<String, Object> input, Map<String, Object> output, PolicyEvaluation policy,
                                        ProvisionPreview preview, String errorCategory) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("redactedInputSummary", input);
        details.put("redactedOutputSummary", output);
        if (policy != null) {
            details.put("policyDecision", policy.decision().name());
            details.put("policyReason", policy.reason());
            details.put("policyRef", policy.policyRef());
        }
        if (preview != null) {
            details.put("previewId", preview.previewId());
            details.put("preview", preview.redactedDetails());
        }
        if (errorCategory != null) {
            details.put("errorCategory", errorCategory);
        }
        return Map.copyOf(details);
    }

    private Duration elapsed(Instant startedAt) {
        return Duration.between(startedAt, clock.instant()).abs();
    }

    private String redactSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "tool execution produced no summary";
        }
        return summary.replaceAll("(?i)secret[-_a-zA-Z0-9]*", "[REDACTED]");
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private RunEventType terminalEventType(ToolExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> RunEventType.TOOL_COMPLETED;
            case CANCELLED -> RunEventType.TOOL_CANCELLED;
            case DENIED, APPROVAL_REQUIRED, SANDBOX_REQUIRED -> RunEventType.TOOL_DENIED;
            default -> RunEventType.TOOL_FAILED;
        };
    }

    private String auditAction(ToolExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> "tool.completed";
            case CANCELLED -> "tool.cancelled";
            case DENIED -> "tool.denied";
            case APPROVAL_REQUIRED -> "tool.approval_required";
            case SANDBOX_REQUIRED -> "tool.sandbox_required";
            default -> "tool.failed";
        };
    }

    private Set<String> union(Set<String> first, Set<String> second) {
        java.util.LinkedHashSet<String> union = new java.util.LinkedHashSet<>(first);
        union.addAll(second);
        return Set.copyOf(union);
    }

    @Override
    public String toString() {
        return "DefaultToolExecutionGateway[toolRegistry=" + toolRegistry.getClass().getSimpleName()
                + ", auditRepository=" + auditRepository.getClass().getSimpleName()
                + ", eventSink=" + eventSink.getClass().getSimpleName() + "]";
    }
}
