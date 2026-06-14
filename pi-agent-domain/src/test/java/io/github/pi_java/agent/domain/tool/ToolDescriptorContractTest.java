package io.github.pi_java.agent.domain.tool;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ToolDescriptorContractTest {

    @Test
    void descriptor_rejects_blank_identity_and_null_governance_metadata() {
        ToolSchema inputSchema = new ToolSchema(
                "https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object"),
                Set.of(),
                4096);
        ToolProvenance provenance = new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "pi", "builtin.info", Map.of());

        assertThatIllegalArgumentException().isThrownBy(() -> new ToolDescriptor(
                " ", "Info", "desc", inputSchema, (ToolSchema) null, provenance, "1.0.0",
                Set.of("info:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", " ", "desc", inputSchema, (ToolSchema) null, provenance, "1.0.0",
                Set.of("info:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", "Info", "desc", inputSchema, (ToolSchema) null, provenance, " ",
                Set.of("info:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));

        assertThatNullPointerException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", "Info", "desc", null, (ToolSchema) null, provenance, "1.0.0",
                Set.of("info:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));
        assertThatNullPointerException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", "Info", "desc", inputSchema, (ToolSchema) null, null, "1.0.0",
                Set.of("info:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));
        assertThatNullPointerException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", "Info", "desc", inputSchema, (ToolSchema) null, provenance, "1.0.0",
                Set.of("info:read"), null, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of()));
        assertThatNullPointerException().isThrownBy(() -> new ToolDescriptor(
                "tool.info", "Info", "desc", inputSchema, (ToolSchema) null, provenance, "1.0.0",
                Set.of("info:read"), ToolRiskLevel.LOW, null, Duration.ofSeconds(5), Map.of()));
    }

    @Test
    void descriptor_carries_schema_scope_timeout_risk_side_effect_and_sensitive_metadata() {
        ToolSchema inputSchema = new ToolSchema(
                "https://json-schema.org/draft/2020-12/schema",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string"),
                                "apiKey", Map.of("type", "string", "x-pi-sensitive", true))),
                Set.of("apiKey"),
                8192);
        ToolSchema outputSchema = new ToolSchema(
                "https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object"),
                Set.of("token"),
                16384);
        ToolProvenance provenance = new ToolProvenance(
                ToolProvenance.SourceKind.MCP,
                "server-a",
                "tools/search",
                Map.of("transport", "stdio"));

        ToolDescriptor descriptor = new ToolDescriptor(
                "search.web",
                "Web Search",
                "Searches a configured provider",
                inputSchema,
                outputSchema,
                provenance,
                "2026.1",
                Set.of("web:read", "tenant:default"),
                ToolRiskLevel.MEDIUM,
                ToolSideEffect.EXTERNAL_READ,
                Duration.ofSeconds(30),
                Map.of("outputType", "application/json"));

        assertThat(descriptor.id()).isEqualTo("search.web");
        assertThat(descriptor.inputSchema().sensitiveFields()).containsExactly("apiKey");
        assertThat(descriptor.outputSchema()).contains(outputSchema);
        assertThat(descriptor.provenance().sourceKind()).isEqualTo(ToolProvenance.SourceKind.MCP);
        assertThat(descriptor.scopes()).containsExactlyInAnyOrder("web:read", "tenant:default");
        assertThat(descriptor.riskLevel()).isEqualTo(ToolRiskLevel.MEDIUM);
        assertThat(descriptor.sideEffect()).isEqualTo(ToolSideEffect.EXTERNAL_READ);
        assertThat(descriptor.defaultTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(descriptor.metadata()).containsEntry("outputType", "application/json");
    }

    @Test
    void execution_result_distinguishes_governance_outcomes_without_raw_payload_requirements() {
        assertThat(ToolExecutionStatus.values()).contains(
                ToolExecutionStatus.SUCCESS,
                ToolExecutionStatus.FAILED,
                ToolExecutionStatus.DENIED,
                ToolExecutionStatus.APPROVAL_REQUIRED,
                ToolExecutionStatus.SANDBOX_REQUIRED,
                ToolExecutionStatus.CANCELLED,
                ToolExecutionStatus.TIMED_OUT,
                ToolExecutionStatus.PREVIEW_ONLY);

        ToolExecutionResult result = new ToolExecutionResult(
                "call-1",
                "search.web",
                ToolExecutionStatus.APPROVAL_REQUIRED,
                "approval required before external call",
                "POLICY_APPROVAL_REQUIRED",
                Map.of("query", "weather", "apiKey", "<redacted>"),
                Map.of("status", "waiting"),
                Set.of("apiKey"),
                new ProvisionPreview("preview-1", "Would call external web search", Set.of("network"), true, Map.of("query", "weather")),
                Duration.ofMillis(12));

        assertThat(result.status()).isEqualTo(ToolExecutionStatus.APPROVAL_REQUIRED);
        assertThat(result.errorCategory()).contains("POLICY_APPROVAL_REQUIRED");
        assertThat(result.redactedInputSummary()).containsEntry("apiKey", "<redacted>");
        assertThat(result.rawOutput()).isEmpty();
        assertThat(result.preview()).isPresent();
    }
}
