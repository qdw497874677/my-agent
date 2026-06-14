package io.github.pi_java.agent.app.port;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
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

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryAppPortContractTest {

    @Test
    void registryListsDescriptorsAndResolvesNamespacedToolIdToDescriptorAndExecutorBinding() {
        ToolDescriptor descriptor = descriptor("builtin:time.now");
        ToolExecutorBinding executor = (request, cancellationToken) -> new ToolExecutionResult(
                request.toolCallId(),
                request.toolId(),
                ToolExecutionStatus.SUCCESS,
                "time returned",
                null,
                Map.of("timezone", "UTC"),
                Map.of("iso", "2026-06-14T00:00:00Z"),
                Set.of(),
                null,
                Duration.ofMillis(3));
        ToolRegistry registry = new InMemoryToolRegistry(descriptor, executor);

        assertThat(registry.listTools()).extracting(ToolDescriptor::id).containsExactly("builtin:time.now");

        Optional<ToolRegistry.ToolResolution> resolution = registry.resolve("builtin:time.now");

        assertThat(resolution).isPresent();
        assertThat(resolution.orElseThrow().descriptor()).isEqualTo(descriptor);
        assertThat(resolution.orElseThrow().executor()).isSameAs(executor);
        assertThat(resolution.orElseThrow().executor().execute(request("builtin:time.now"), new CancellationToken()).status())
                .isEqualTo(ToolExecutionStatus.SUCCESS);
    }

    @Test
    void unknownToolResolutionReturnsEmptyRatherThanNull() {
        ToolRegistry registry = new InMemoryToolRegistry(descriptor("builtin:time.now"), (request, cancellationToken) -> null);

        Optional<ToolRegistry.ToolResolution> resolution = registry.resolve("missing:tool");

        assertThat(resolution).isEmpty();
    }

    @Test
    void registryApiIsDescriptorFirstAndNotSourceSpecific() {
        List<String> methodNames = Arrays.stream(ToolRegistry.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(methodNames).contains("listTools", "resolve");
        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("spring"));
        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("mcp"));
        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("plugin"));
        assertThat(methodNames).doesNotContain("registerSpringBean", "registerMcpTool", "registerPluginTool");
    }

    private static ToolExecutionRequest request(String toolId) {
        return new ToolExecutionRequest("call-1", new RunId("run-1"), new StepId("step-1"), toolId, "1.0.0",
                Map.of("timezone", "UTC"), Instant.parse("2026-06-14T00:00:00Z"));
    }

    private static ToolDescriptor descriptor(String id) {
        ToolSchema inputSchema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object", "properties", Map.of("timezone", Map.of("type", "string"))),
                Set.of(),
                4096);
        ToolSchema outputSchema = new ToolSchema("https://json-schema.org/draft/2020-12/schema",
                Map.of("type", "object", "properties", Map.of("iso", Map.of("type", "string"))),
                Set.of(),
                4096);
        return new ToolDescriptor(
                id,
                "Current time",
                "Returns the current time for a timezone.",
                inputSchema,
                outputSchema,
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin-tools", "time-now", Map.of()),
                "1.0.0",
                Set.of("time:read"),
                ToolRiskLevel.LOW,
                ToolSideEffect.READ_ONLY,
                Duration.ofSeconds(2),
                Map.of("category", "utility"));
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
