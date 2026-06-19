package io.github.pi_java.agent.app.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.observability.OperationsMetricsReader;
import io.github.pi_java.agent.app.port.plugin.EmptyPluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.OperationMetricDto;
import io.github.pi_java.agent.client.admin.OperationalWarningDto;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultGovernanceQueryServiceTest {

    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-a", "admin-a", java.util.Set.of("ADMIN")),
            new CorrelationContext("1234567890abcdef1234567890abcdef", "corr-a", null));

    @Test
    void operationsDelegatesToOperationsMetricsReader() {
        OperationsSummaryResponse expected = new OperationsSummaryResponse(
                List.of(new OperationMetricDto("runs", "events", "HEALTHY", 3.0, "count", Map.of("source", "run-event"))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new OperationalWarningDto("runs", "WARN", "Recent run failures detected", Map.of("count", "1"))),
                Instant.parse("2026-06-19T00:00:00Z"));
        CapturingOperationsMetricsReader reader = new CapturingOperationsMetricsReader(expected);

        DefaultGovernanceQueryService service = serviceWith(reader);

        assertThat(service.operations(CONTEXT)).isSameAs(expected);
        assertThat(reader.context).isSameAs(CONTEXT);
    }

    @Test
    void operationsUsesEmptyFallbackWhenReaderIsMissing() {
        DefaultGovernanceQueryService service = serviceWith(null);

        OperationsSummaryResponse response = service.operations(CONTEXT);

        assertThat(response.runs()).isEmpty();
        assertThat(response.models()).isEmpty();
        assertThat(response.tools()).isEmpty();
        assertThat(response.policies()).isEmpty();
        assertThat(response.mcp()).isEmpty();
        assertThat(response.plugins()).isEmpty();
        assertThat(response.errors()).isEmpty();
        assertThat(response.warnings()).isEmpty();
        assertThat(response.generatedAt()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
    }

    private static DefaultGovernanceQueryService serviceWith(OperationsMetricsReader reader) {
        return new DefaultGovernanceQueryService(
                List::of,
                new ToolRegistry() {
                    @Override
                    public List<ToolDescriptor> listTools() {
                        return List.of();
                    }

                    @Override
                    public Optional<ToolResolution> resolve(String toolId) {
                        return Optional.empty();
                    }
                },
                () -> List.of(),
                new io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog.EmptyMcpGovernanceCatalog(),
                new EmptyPluginGovernanceCatalog(),
                Optional.ofNullable(reader),
                Optional.empty(),
                Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class CapturingOperationsMetricsReader implements OperationsMetricsReader {
        private final OperationsSummaryResponse response;
        private RequestContext context;

        private CapturingOperationsMetricsReader(OperationsSummaryResponse response) {
            this.response = response;
        }

        @Override
        public OperationsSummaryResponse summarize(RequestContext context) {
            this.context = context;
            return response;
        }
    }
}
