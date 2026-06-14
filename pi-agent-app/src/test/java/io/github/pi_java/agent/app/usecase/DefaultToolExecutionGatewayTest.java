package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator;
import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload.ToolLifecyclePayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultToolExecutionGatewayTest {

    @Test
    void allowPathValidatesAuditsEmitsEventsInvokesOnceAndReturnsRedactedSuccess() {
        AtomicInteger invocations = new AtomicInteger();
        Harness harness = Harness.create((request, cancellationToken) -> {
            invocations.incrementAndGet();
            return result(request, ToolExecutionStatus.SUCCESS, "raw secret should be replaced",
                    Map.of("value", "secret-output"), null);
        });

        ToolExecutionResult result = harness.gateway.execute(harness.command());

        assertThat(invocations).hasValue(1);
        assertThat(result.status()).isEqualTo(ToolExecutionStatus.SUCCESS);
        assertThat(result.summary()).isEqualTo("raw [REDACTED] should be replaced");
        assertThat(result.redactedInputSummary()).containsEntry("apiKey", "[REDACTED]");
        assertThat(result.redactedOutputSummary()).containsEntry("value", "[REDACTED]");
        assertThat(harness.eventTypes()).containsSubsequence(
                RunEventType.TOOL_PROPOSED,
                RunEventType.TOOL_POLICY_DECIDED,
                RunEventType.TOOL_STARTED,
                RunEventType.TOOL_COMPLETED);
        assertThat(harness.auditActions()).contains("tool.policy_decided", "tool.started", "tool.completed");
    }

    @Test
    void invalidArgumentsFailWithoutInvokingExecutor() {
        AtomicInteger invocations = new AtomicInteger();
        Harness harness = Harness.create((request, cancellationToken) -> {
            invocations.incrementAndGet();
            return result(request, ToolExecutionStatus.SUCCESS, "should not execute", Map.of(), null);
        });
        harness.validator = (descriptor, request) -> ToolArgumentValidator.ValidationResult.invalid(
                "invalid_args", "timezone is required", Map.of("field", "timezone"));

        ToolExecutionResult result = harness.rebuild().gateway.execute(harness.command());

        assertThat(invocations).hasValue(0);
        assertThat(result.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(result.errorCategory()).contains("invalid_args");
        assertThat(harness.eventTypes()).contains(RunEventType.TOOL_FAILED);
        assertThat(harness.auditActions()).contains("tool.validation_failed");
    }

    @Test
    void denyBlockApprovalAndSandboxDecisionsNeverInvokeExecutor() {
        for (PolicyDecision decision : List.of(PolicyDecision.DENY, PolicyDecision.BLOCK,
                PolicyDecision.REQUIRE_APPROVAL, PolicyDecision.REQUIRE_SANDBOX)) {
            AtomicInteger invocations = new AtomicInteger();
            Harness harness = Harness.create((request, cancellationToken) -> {
                invocations.incrementAndGet();
                return result(request, ToolExecutionStatus.SUCCESS, "should not execute", Map.of(), null);
            });
            harness.policy = ignored -> new ToolPolicyEvaluator.PolicyEvaluation(
                    decision, "policy says " + decision, "policy:test", false, "approval-1", "sandbox-1", Map.of());

            ToolExecutionResult result = harness.rebuild().gateway.execute(harness.command());

            assertThat(invocations).as(decision.name()).hasValue(0);
            if (decision == PolicyDecision.REQUIRE_APPROVAL) {
                assertThat(result.status()).isEqualTo(ToolExecutionStatus.APPROVAL_REQUIRED);
                assertThat(harness.eventTypes()).contains(RunEventType.TOOL_APPROVAL_REQUIRED);
            } else if (decision == PolicyDecision.REQUIRE_SANDBOX) {
                assertThat(result.status()).isEqualTo(ToolExecutionStatus.SANDBOX_REQUIRED);
                assertThat(harness.eventTypes()).contains(RunEventType.TOOL_DENIED);
            } else {
                assertThat(result.status()).isEqualTo(ToolExecutionStatus.DENIED);
                assertThat(harness.eventTypes()).contains(RunEventType.TOOL_DENIED);
            }
            assertThat(harness.auditActions()).contains("tool.policy_decided");
        }
    }

    @Test
    void sideEffectfulOrPolicyRequestedPreviewIsGeneratedBeforeExecutionAndRedacted() {
        AtomicInteger invocations = new AtomicInteger();
        Harness harness = Harness.create((request, cancellationToken) -> {
            invocations.incrementAndGet();
            return result(request, ToolExecutionStatus.SUCCESS, "updated", Map.of("ok", true), null);
        });
        harness.policy = ignored -> new ToolPolicyEvaluator.PolicyEvaluation(
                PolicyDecision.ALLOW, "preview first", "policy:test", true, Optional.empty(), Optional.empty(), Map.of());

        ToolExecutionResult result = harness.rebuild().gateway.execute(harness.command());

        assertThat(invocations).hasValue(1);
        assertThat(result.preview()).isPresent();
        assertThat(result.preview().orElseThrow().redactedDetails()).containsEntry("apiKey", "[REDACTED]");
        assertThat(harness.eventTypes()).containsSubsequence(RunEventType.TOOL_PREVIEW_GENERATED, RunEventType.TOOL_STARTED);
        assertThat(harness.auditActions()).contains("tool.preview_generated");
    }

    @Test
    void cancellationBeforeExecutionAndExecutorTimeoutOrFailureNormalizeOutcomes() {
        Harness cancelled = Harness.create((request, cancellationToken) -> result(request, ToolExecutionStatus.SUCCESS, "no", Map.of(), null));
        CancellationToken token = new CancellationToken();
        token.cancel("user requested");
        ToolExecutionResult cancelledResult = cancelled.gateway.execute(cancelled.command(token));
        assertThat(cancelledResult.status()).isEqualTo(ToolExecutionStatus.CANCELLED);
        assertThat(cancelled.eventTypes()).contains(RunEventType.TOOL_CANCELLED);

        Harness timeout = Harness.create((request, cancellationToken) -> {
            throw new RuntimeException(new TimeoutException("deadline"));
        });
        ToolExecutionResult timeoutResult = timeout.gateway.execute(timeout.command());
        assertThat(timeoutResult.status()).isEqualTo(ToolExecutionStatus.TIMED_OUT);
        assertThat(timeout.eventTypes()).contains(RunEventType.TOOL_FAILED);

        Harness failed = Harness.create((request, cancellationToken) -> {
            throw new IllegalStateException("boom secret");
        });
        ToolExecutionResult failedResult = failed.gateway.execute(failed.command());
        assertThat(failedResult.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(failedResult.summary()).doesNotContain("secret");
        assertThat(failed.auditActions()).contains("tool.failed");
    }

    private static ToolExecutionResult result(ToolExecutionRequest request, ToolExecutionStatus status, String summary,
                                             Map<String, Object> output, ProvisionPreview preview) {
        return new ToolExecutionResult(request.toolCallId(), request.toolId(), status, summary, Optional.empty(),
                request.arguments(), output, Set.of(), Optional.ofNullable(preview), Duration.ofMillis(10), Optional.of(output));
    }

    private static final class Harness {
        final RecordingEventSink eventSink = new RecordingEventSink();
        final RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        final ToolExecutorBinding executor;
        ToolArgumentValidator validator = (descriptor, request) -> ToolArgumentValidator.ValidationResult.ok();
        ToolPolicyEvaluator policy = ignored -> ToolPolicyEvaluator.PolicyEvaluation.allow("safe", "policy:test");
        ToolPreviewGenerator preview = request -> new ProvisionPreview("preview-1", "will update resource",
                Set.of("workspace-write"), true, Map.of("apiKey", "[REDACTED]"));
        ToolRedactor redactor = (descriptor, payload) -> new ToolRedactor.RedactedToolPayload(redact(payload), Set.of("apiKey", "value"), false);
        ToolPayloadLimiter limiter = new ToolPayloadLimiter() {
            @Override
            public LimitCheck checkArguments(ToolDescriptor descriptor, Map<String, Object> arguments) {
                return LimitCheck.ok();
            }

            @Override
            public LimitCheck checkResult(ToolDescriptor descriptor, Map<String, Object> result) {
                return LimitCheck.ok();
            }

            @Override
            public Map<String, Object> summarize(ToolDescriptor descriptor, Map<String, Object> payload) {
                return payload;
            }
        };
        ToolExecutionGateway gateway;

        private Harness(ToolExecutorBinding executor) {
            this.executor = executor;
            rebuild();
        }

        static Harness create(ToolExecutorBinding executor) {
            return new Harness(executor);
        }

        Harness rebuild() {
            ToolRegistry registry = new SingleToolRegistry(descriptor(), executor);
            gateway = new DefaultToolExecutionGateway(registry, validator, policy, redactor, limiter, preview,
                    auditRepository, eventSink, Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneOffset.UTC));
            return this;
        }

        ToolExecutionGateway.ToolExecutionCommand command() {
            return command(new CancellationToken());
        }

        ToolExecutionGateway.ToolExecutionCommand command(CancellationToken token) {
            return new ToolExecutionGateway.ToolExecutionCommand(context(), new SessionId("session-1"),
                    new WorkspaceId("workspace-1"), request(), token);
        }

        List<RunEventType> eventTypes() {
            return eventSink.events.stream().map(RunEvent::type).toList();
        }

        List<String> auditActions() {
            return auditRepository.records.stream().map(AuditRecord::action).toList();
        }
    }

    private static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of("agent")),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static ToolExecutionRequest request() {
        return new ToolExecutionRequest("call-1", new RunId("run-1"), new StepId("step-1"), "builtin:update",
                "1.0.0", Map.of("timezone", "UTC", "apiKey", "secret-input"), Instant.parse("2026-06-14T00:00:00Z"));
    }

    private static ToolDescriptor descriptor() {
        ToolSchema schema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object"), Set.of("apiKey"), 4096);
        return new ToolDescriptor("builtin:update", "Update", "Updates workspace resource", schema, schema,
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin", "update", Map.of()),
                "1.0.0", Set.of("workspace:write"), ToolRiskLevel.MEDIUM, ToolSideEffect.WORKSPACE_WRITE,
                Duration.ofSeconds(2), Map.of());
    }

    private static Map<String, Object> redact(Map<String, Object> payload) {
        return payload.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                entry -> entry.getKey().equals("apiKey") || entry.getKey().equals("value") ? "[REDACTED]" : entry.getValue()));
    }

    private record SingleToolRegistry(ToolDescriptor descriptor, ToolExecutorBinding executor) implements ToolRegistry {
        @Override
        public List<ToolDescriptor> listTools() {
            return List.of(descriptor);
        }

        @Override
        public Optional<ToolResolution> resolve(String toolId) {
            return descriptor.id().equals(toolId) ? Optional.of(new ToolResolution(descriptor, executor)) : Optional.empty();
        }
    }

    private static final class RecordingEventSink implements EventSink {
        final List<RunEvent> events = new ArrayList<>();

        @Override
        public void publish(RunEvent event) {
            events.add(event);
            assertThat(event.payload()).isInstanceOf(ToolLifecyclePayload.class);
        }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        final List<AuditRecord> records = new ArrayList<>();

        @Override
        public void record(RequestContext context, String action, String resourceType, String resourceId,
                           String sessionId, String runId, Map<String, Object> details) {
            records.add(new AuditRecord(action, resourceType, resourceId, details));
        }
    }

    private record AuditRecord(String action, String resourceType, String resourceId, Map<String, Object> details) {
    }
}
