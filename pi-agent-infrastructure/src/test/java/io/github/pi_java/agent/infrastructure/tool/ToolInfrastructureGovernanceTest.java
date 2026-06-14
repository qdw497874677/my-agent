package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
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
        return new ToolDescriptor(
                id,
                "Echo",
                "Echoes safe input",
                inputSchema,
                java.util.Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtins", "echo", Map.of()),
                "1.0.0",
                Set.of("tool:read"),
                ToolRiskLevel.LOW,
                ToolSideEffect.READ_ONLY,
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
}
