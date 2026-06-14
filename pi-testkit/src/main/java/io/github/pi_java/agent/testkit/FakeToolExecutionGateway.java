package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator;
import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.app.usecase.DefaultToolExecutionGateway;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class FakeToolExecutionGateway implements ToolExecutionGateway {
    private final ToolExecutionGateway delegate;

    public FakeToolExecutionGateway(ToolRegistry registry, FakePolicy policy, EventSink eventSink, Clock clock) {
        Objects.requireNonNull(policy, "policy must not be null");
        this.delegate = new DefaultToolExecutionGateway(
                registry,
                (descriptor, request) -> ToolArgumentValidator.ValidationResult.ok(),
                request -> {
                    PolicyDecision decision = policy.decide(toLegacyToolCall(request.toolRequest()));
                    return new ToolPolicyEvaluator.PolicyEvaluation(decision, "fake policy decision", "fake-policy",
                            decision == PolicyDecision.REQUIRE_APPROVAL, Optional.empty(), Optional.empty(), Map.of());
                },
                FakeToolExecutionGateway::redact,
                new AllowAllPayloadLimiter(),
                request -> new ProvisionPreview("fake-preview-" + request.toolRequest().toolCallId(), "fake preview",
                        Set.of("policy approval"), true, Map.of("toolId", request.descriptor().id())),
                NOOP_AUDIT,
                eventSink,
                clock == null ? Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC) : clock);
    }

    public static FakeToolExecutionGateway fromInvoker(FakeToolInvoker invoker, FakePolicy policy, EventSink eventSink,
                                                       io.github.pi_java.agent.domain.runtime.RunContext context,
                                                       Clock clock) {
        FakeToolRegistry registry = new FakeToolRegistry();
        invoker.registeredToolNames().forEach(toolName -> registry.register(toolName, new FakeToolExecutorBinding(invoker, context)));
        return new FakeToolExecutionGateway(registry, policy, eventSink, clock);
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionCommand command) {
        return delegate.execute(command);
    }

    private static io.github.pi_java.agent.domain.tool.ToolCall toLegacyToolCall(ToolExecutionRequest request) {
        return new io.github.pi_java.agent.domain.tool.ToolCall(request.toolCallId(), request.runId(), request.stepId(),
                request.toolId(), request.arguments(), request.requestedAt());
    }

    private static ToolRedactor.RedactedToolPayload redact(ToolDescriptor descriptor, Map<String, Object> payload) {
        return new ToolRedactor.RedactedToolPayload(payload, Set.of(), false);
    }

    private static final AuditRepository NOOP_AUDIT = (context, action, resourceType, resourceId, sessionId, runId, details) -> {
    };

    private static final class AllowAllPayloadLimiter implements ToolPayloadLimiter {
        private final AtomicLong bytes = new AtomicLong();

        @Override
        public LimitCheck checkArguments(ToolDescriptor descriptor, Map<String, Object> arguments) {
            estimate(arguments);
            return LimitCheck.ok();
        }

        @Override
        public LimitCheck checkResult(ToolDescriptor descriptor, Map<String, Object> result) {
            estimate(result);
            return LimitCheck.ok();
        }

        @Override
        public Map<String, Object> summarize(ToolDescriptor descriptor, Map<String, Object> payload) {
            return payload;
        }

        private long estimate(Map<String, Object> payload) {
            return bytes.addAndGet(payload.toString().length());
        }
    }
}
