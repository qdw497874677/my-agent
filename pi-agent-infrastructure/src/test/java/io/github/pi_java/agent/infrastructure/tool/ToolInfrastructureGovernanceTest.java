package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.app.port.tool.ToolRedactor;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolInfrastructureGovernanceTest {

    @Test
    void validatesArgumentsWithJsonSchemaAndReturnsSafeErrors() {
        ToolDescriptor descriptor = descriptor("builtin.echo", inputSchema());
        NetworkntToolArgumentValidator validator = new NetworkntToolArgumentValidator();

        ToolArgumentValidator.ValidationResult valid = validator.validate(descriptor, request(Map.of("message", "hello")));

        assertThat(valid.valid()).isTrue();

        ToolArgumentValidator.ValidationResult invalid = validator.validate(descriptor, request(Map.of("secret", "raw-secret-value")));

        assertThat(invalid.valid()).isFalse();
        assertThat(invalid.errorCode()).contains("TOOL_ARGUMENT_SCHEMA_INVALID");
        assertThat(invalid.message()).isEqualTo("tool arguments failed schema validation");
        assertThat(invalid.redactedDetails()).containsKeys("schemaDialect", "errorCount", "errors");
        assertThat(invalid.redactedDetails().toString())
                .doesNotContain("raw-secret-value")
                .doesNotContain("secret=raw-secret-value");
    }

    @Test
    void inMemoryRegistryResolvesByDescriptorIdAndRejectsDuplicates() {
        ToolDescriptor descriptor = descriptor("builtin.echo", inputSchema());
        InMemoryToolRegistry.ToolRegistration registration = new InMemoryToolRegistry.ToolRegistration(
                descriptor,
                (request, cancellationToken) -> new ToolExecutionResult(
                        request.toolCallId(),
                        request.toolId(),
                        ToolExecutionStatus.SUCCESS,
                        "ok",
                        null,
                        Map.of(),
                        Map.of("ok", true),
                        Set.of(),
                        null,
                        Duration.ZERO
                )
        );
        InMemoryToolRegistry registry = new InMemoryToolRegistry(java.util.List.of(registration));

        assertThat(registry.listTools()).containsExactly(descriptor);
        assertThat(registry.resolve("builtin.echo")).isPresent();
        assertThat(registry.resolve("missing.tool")).isEmpty();
        assertThat(registry.resolve(" ")).isEmpty();

        assertThatThrownBy(() -> new InMemoryToolRegistry(java.util.List.of(registration, registration)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate tool id: builtin.echo");
    }

    @Test
    void networkntTypesAreInfrastructureOnly() throws Exception {
        java.nio.file.Path root = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (root.getFileName() != null && root.getFileName().toString().equals("pi-agent-infrastructure")) {
            root = root.getParent();
        }

        assertThat(sourceContains(root.resolve("pi-agent-infrastructure/src/main/java"), "com.networknt")).isTrue();
        assertThat(sourceContains(root.resolve("pi-agent-domain/src/main/java"), "com.networknt")).isFalse();
        assertThat(sourceContains(root.resolve("pi-agent-app/src/main/java"), "com.networknt")).isFalse();
    }

    @Test
    void conservativePolicyAllowsOnlySafeAllowedScopesByDefault() {
        DefaultToolPolicyEvaluator evaluator = new DefaultToolPolicyEvaluator(agentDefinition(Set.of("tool:read")));

        ToolPolicyEvaluator.PolicyEvaluation safe = evaluator.evaluate(policyRequest(
                descriptor("builtin.read", inputSchema(), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Set.of("tool:read"))));
        ToolPolicyEvaluator.PolicyEvaluation write = evaluator.evaluate(policyRequest(
                descriptor("builtin.write", inputSchema(), ToolRiskLevel.MEDIUM, ToolSideEffect.WORKSPACE_WRITE, Set.of("tool:write"))));
        ToolPolicyEvaluator.PolicyEvaluation destructive = evaluator.evaluate(policyRequest(
                descriptor("builtin.delete", inputSchema(), ToolRiskLevel.CRITICAL, ToolSideEffect.DESTRUCTIVE, Set.of("tool:write"))));

        assertThat(safe.decision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(safe.previewRequired()).isFalse();
        assertThat(write.decision()).isEqualTo(PolicyDecision.REQUIRE_APPROVAL);
        assertThat(write.previewRequired()).isTrue();
        assertThat(destructive.decision()).isEqualTo(PolicyDecision.BLOCK);
    }

    @Test
    void redactorMasksSensitiveFieldsAndSecretLikeValues() {
        ToolDescriptor descriptor = descriptor("builtin.secret", new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of(
                "type", "object",
                "properties", Map.of("apiKey", Map.of("type", "string", "x-sensitive", true))
        ), Set.of("apiKey"), 4096));
        DefaultToolRedactor redactor = new DefaultToolRedactor(1024);

        ToolRedactor.RedactedToolPayload redacted = redactor.redact(descriptor, Map.of(
                "apiKey", "sk-live-secret",
                "nested", Map.of("password", "p@ssword", "normal", "value"),
                "credential", "CredentialRef:prod/db"
        ));

        assertThat(redacted.summary().toString())
                .doesNotContain("sk-live-secret")
                .doesNotContain("p@ssword")
                .doesNotContain("prod/db")
                .contains("[REDACTED]");
        assertThat(redacted.redactedFields()).contains("apiKey", "nested.password", "credential");
    }

    @Test
    void payloadLimiterSummarizesOversizedPayloadsWithMetadata() {
        DefaultToolPayloadLimiter limiter = new DefaultToolPayloadLimiter(64, 64, 24);
        ToolDescriptor descriptor = descriptor("builtin.large", inputSchema());
        Map<String, Object> payload = Map.of("body", "x".repeat(200));

        assertThat(limiter.checkArguments(descriptor, payload).allowed()).isFalse();
        Map<String, Object> summary = limiter.summarize(descriptor, payload);

        assertThat(summary).containsEntry("truncated", true);
        assertThat(summary).containsKeys("estimatedBytes", "limitBytes", "payloadPreview");
        assertThat(summary.get("payloadPreview").toString()).hasSizeLessThanOrEqualTo(27);
    }

    @Test
    void previewGeneratorProducesImpactEstimateOnly() {
        DefaultToolPreviewGenerator generator = new DefaultToolPreviewGenerator();
        ToolDescriptor descriptor = descriptor("builtin.write", inputSchema(), ToolRiskLevel.MEDIUM,
                ToolSideEffect.WORKSPACE_WRITE, Set.of("tool:write"));
        ToolPolicyEvaluator.PolicyEvaluation policy = new ToolPolicyEvaluator.PolicyEvaluation(
                PolicyDecision.REQUIRE_APPROVAL, "side effects require approval", "default-tool-policy", true,
                "approval-1", null, Map.of());

        ProvisionPreview preview = generator.generate(new io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator.PreviewRequest(
                requestContext(), new SessionId("session-1"), new WorkspaceId("workspace-1"), descriptor, request(Map.of("message", "write")),
                policy, Map.of("message", "write")));

        assertThat(preview.previewId()).startsWith("preview-");
        assertThat(preview.summary()).contains("WORKSPACE_WRITE");
        assertThat(preview.redactedDetails()).containsEntry("executesSideEffects", false);
    }

    private boolean sourceContains(java.nio.file.Path directory, String needle) throws Exception {
        if (!java.nio.file.Files.exists(directory)) {
            return false;
        }
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> {
                        try {
                            return java.nio.file.Files.readString(path).contains(needle);
                        } catch (java.io.IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
        }
    }

    private ToolExecutionRequest request(Map<String, Object> arguments) {
        return new ToolExecutionRequest(
                "call-1",
                new RunId("run-1"),
                new StepId("step-1"),
                "builtin.echo",
                "1.0.0",
                arguments,
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private ToolDescriptor descriptor(String id, ToolSchema inputSchema) {
        return descriptor(id, inputSchema, ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Set.of("tool:read"));
    }

    private ToolDescriptor descriptor(String id, ToolSchema inputSchema, ToolRiskLevel riskLevel, ToolSideEffect sideEffect,
                                      Set<String> scopes) {
        return new ToolDescriptor(
                id,
                "Echo",
                "Echoes safe input",
                inputSchema,
                java.util.Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtins", "echo", Map.of()),
                "1.0.0",
                scopes,
                riskLevel,
                sideEffect,
                Duration.ofSeconds(5),
                Map.of()
        );
    }

    private ToolSchema inputSchema() {
        return new ToolSchema("https://json-schema.org/draft/2020-12/schema", Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", java.util.List.of("message"),
                "properties", Map.of(
                        "message", Map.of("type", "string", "minLength", 1)
                )
        ), Set.of("secret"), 4096);
    }

    private ToolPolicyEvaluator.PolicyEvaluationRequest policyRequest(ToolDescriptor descriptor) {
        return new ToolPolicyEvaluator.PolicyEvaluationRequest(requestContext(), new SessionId("session-1"),
                new WorkspaceId("workspace-1"), descriptor, request(Map.of("message", "hello")), Map.of("message", "hello"));
    }

    private RequestContext requestContext() {
        return new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of("agent:run")),
                new CorrelationContext("trace-1", "correlation-1", "causation-1")
        );
    }

    private AgentDefinition agentDefinition(Set<String> allowedScopes) {
        return new AgentDefinition(new AgentId("agent-1"), "Agent", "Instructions", "fake:model", allowedScopes,
                Set.of("policy-default"), new RuntimeLimits(Duration.ofMinutes(1), 10, 5), Set.of(InteractionMode.CHAT),
                "workspace-default", "output-default");
    }
}
