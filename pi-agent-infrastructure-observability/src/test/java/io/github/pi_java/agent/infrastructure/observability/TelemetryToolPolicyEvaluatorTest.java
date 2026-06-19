package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluation;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator.PolicyEvaluationRequest;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryToolPolicyEvaluatorTest {

    private static final String SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void records_decision_metrics_without_raw_tool_arguments() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryToolPolicyEvaluator evaluator = new TelemetryToolPolicyEvaluator(request ->
                new PolicyEvaluation(PolicyDecision.ALLOW, "safe", "default-tool-policy", false,
                        Optional.empty(), Optional.empty(), Map.of("raw", SECRET)), new PiTelemetry(registry, null));

        PolicyEvaluation evaluation = evaluator.evaluate(request(Map.of("apiKey", SECRET)));

        assertThat(evaluation.decision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(registry.get(PiTelemetryNames.POLICY_DECISIONS_TOTAL)
                .tags("tool_id", "demo.tool", "decision", "ALLOW", "policy_ref", "default-tool-policy", "status", "success")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags().stream().map(tag -> tag.getKey() + "=" + tag.getValue()).toList())
                .noneMatch(value -> value.contains(SECRET))
                .noneMatch(value -> value.contains("apiKey"));
    }

    @Test
    void records_error_decision_metric_and_rethrows() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryToolPolicyEvaluator evaluator = new TelemetryToolPolicyEvaluator(request -> {
            throw new IllegalArgumentException("policy failed " + SECRET);
        }, new PiTelemetry(registry, null));

        assertThatThrownBy(() -> evaluator.evaluate(request(Map.of("password", SECRET))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(registry.get(PiTelemetryNames.POLICY_DECISIONS_TOTAL)
                .tags("tool_id", "demo.tool", "decision", "error", "policy_ref", "unknown", "status", "error")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags().stream().map(tag -> tag.getKey() + "=" + tag.getValue()).toList())
                .noneMatch(value -> value.contains(SECRET))
                .noneMatch(value -> value.contains("password"));
    }

    private static PolicyEvaluationRequest request(Map<String, Object> arguments) {
        ToolDescriptor descriptor = new ToolDescriptor("demo.tool", "Demo Tool", "safe demo",
                schema(), Optional.empty(), new ToolProvenance(ToolProvenance.SourceKind.TESTKIT, "test", "demo.tool", Map.of()),
                "v1", Set.of("tool:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5),
                Map.of("source", "test"));
        return new PolicyEvaluationRequest(
                new RequestContext(new SecurityPrincipalContext("tenant-a", "user-a", Set.of("USER")),
                        new CorrelationContext("4bf92f3577b34da6a3ce929d0e0e4736", "corr-a", "cause-a")),
                new SessionId("session-a"),
                new WorkspaceId("workspace-a"),
                descriptor,
                new ToolExecutionRequest("call-a", new RunId("run-a"), new StepId("step-a"), descriptor.id(), descriptor.version(),
                        arguments, Instant.parse("2026-06-19T00:00:00Z")),
                Map.of("redacted", "safe"));
    }

    private static ToolSchema schema() {
        return new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of("type", "object"), Set.of("apiKey"), 4096);
    }
}
