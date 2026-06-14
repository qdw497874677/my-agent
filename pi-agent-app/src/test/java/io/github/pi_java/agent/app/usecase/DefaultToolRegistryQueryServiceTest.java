package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
import io.github.pi_java.agent.client.tool.ToolDescriptorDto;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultToolRegistryQueryServiceTest {

    @Test
    void catalogResponseIncludesDescriptorMetadataRequiredByAdminAndConsole() {
        ToolRegistryQueryService service = new DefaultToolRegistryQueryService(new InMemoryToolRegistry(descriptor(), executor()));

        ToolCatalogResponse catalog = service.listTools(context());

        assertThat(catalog.tools()).hasSize(1);
        ToolDescriptorDto tool = catalog.tools().getFirst();
        assertThat(tool.id()).isEqualTo("builtin:time.now");
        assertThat(tool.name()).isEqualTo("Current time");
        assertThat(tool.description()).isEqualTo("Returns the current time for a timezone.");
        assertThat(tool.inputSchema().document()).containsEntry("type", "object");
        assertThat(tool.outputSchema()).hasValueSatisfying(schema -> assertThat(schema.payloadLimitBytes()).isEqualTo(4096));
        assertThat(tool.provenance().sourceKind()).isEqualTo("BUILT_IN");
        assertThat(tool.provenance().sourceId()).isEqualTo("builtin-tools");
        assertThat(tool.provenance().bindingRef()).isEqualTo("time-now");
        assertThat(tool.version()).isEqualTo("1.0.0");
        assertThat(tool.scopes()).containsExactly("time:read");
        assertThat(tool.riskLevel()).isEqualTo("LOW");
        assertThat(tool.sideEffect()).isEqualTo("READ_ONLY");
        assertThat(tool.defaultTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(tool.metadata()).containsEntry("category", "utility");
    }

    @Test
    void catalogResponseNeverExposesExecutorBindingsRawSecretsOrImplementationObjects() {
        ToolRegistryQueryService service = new DefaultToolRegistryQueryService(new InMemoryToolRegistry(descriptor(), executor()));

        ToolCatalogResponse catalog = service.listTools(context());

        assertThat(catalog.toString())
                .doesNotContain("SecretToolExecutor", "sk-live-super-secret", "ToolExecutorBinding", "lambda");
        assertThat(recordComponentNames(ToolDescriptorDto.class))
                .doesNotContain("executor", "executorBinding", "binding", "implementation", "implementationClass");
        assertThat(recordComponentNames(ToolCatalogResponse.class)).containsExactly("tools");
    }

    private static List<String> recordComponentNames(Class<? extends Record> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
    }

    private static RequestContext context() {
        return new RequestContext(new SecurityPrincipalContext("tenant-1", "user-1", Set.of("admin")),
                new CorrelationContext("trace-1", "correlation-1", "causation-1"));
    }

    private static ToolDescriptor descriptor() {
        ToolSchema inputSchema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object", "properties", Map.of("timezone", Map.of("type", "string"))),
                Set.of("apiKey"),
                4096);
        ToolSchema outputSchema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object", "properties", Map.of("iso", Map.of("type", "string"))),
                Set.of(),
                4096);
        return new ToolDescriptor(
                "builtin:time.now",
                "Current time",
                "Returns the current time for a timezone.",
                inputSchema,
                outputSchema,
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin-tools", "time-now",
                        Map.of("owner", "platform")),
                "1.0.0",
                Set.of("time:read"),
                ToolRiskLevel.LOW,
                ToolSideEffect.READ_ONLY,
                Duration.ofSeconds(2),
                Map.of("category", "utility"));
    }

    private static ToolExecutorBinding executor() {
        return new SecretToolExecutor();
    }

    private static final class SecretToolExecutor implements ToolExecutorBinding {
        @Override
        public ToolExecutionResult execute(io.github.pi_java.agent.domain.tool.ToolExecutionRequest request,
                                           io.github.pi_java.agent.domain.runtime.CancellationToken cancellationToken) {
            return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                    "secret executor should never appear in catalog", null, Map.of(), Map.of(), Set.of(), null,
                    Duration.ZERO);
        }
    }

    private record InMemoryToolRegistry(ToolDescriptor descriptor, ToolExecutorBinding executor) implements ToolRegistry {
        @Override
        public List<ToolDescriptor> listTools() {
            return List.of(descriptor);
        }

        @Override
        public Optional<ToolResolution> resolve(String toolId) {
            if (descriptor.id().equals(toolId)) {
                return Optional.of(new ToolResolution(descriptor, executor));
            }
            return Optional.empty();
        }
    }
}
