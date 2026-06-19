package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.port.extension.ExtensionCapabilityStatus;
import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.extension.ExtensionSourceStatus;
import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.EmptyPluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultGovernanceQueryServiceExtensionTest {

    private static final RequestContext CONTEXT = new RequestContext(
            new SecurityPrincipalContext("tenant-1", "user-1", Set.of("ADMIN")),
            new CorrelationContext("trace-1", "corr-1", null));

    @Test
    void extensionsQueryMapsCatalogSourcesToPublicDtos() {
        DefaultGovernanceQueryService service = serviceWith(catalogWithFailedAndDisabledSource());

        ExtensionGovernanceResponse response = service.extensions(CONTEXT);

        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().getFirst().redactedError()).isEqualTo("<redacted>");
        assertThat(response.sources().getFirst().capabilities()).hasSize(2);
        assertThat(response.sources().getFirst().capabilities().getFirst().metadata())
                .containsEntry("secret", "<redacted>")
                .containsEntry("mutation", "disabled");
    }

    @Test
    void overviewExtensionStatusReflectsCatalogCountsAndHealth() {
        DefaultGovernanceQueryService service = serviceWith(catalogWithFailedAndDisabledSource());

        GovernanceOverviewResponse overview = service.overview(CONTEXT);

        assertThat(overview.extensions().status()).isEqualTo("DEGRADED");
        assertThat(overview.extensions().count()).isEqualTo(2);
        assertThat(overview.extensions().metadata())
                .containsEntry("surface", "read-only")
                .containsEntry("mutation", "disabled")
                .containsEntry("sources", "2")
                .containsEntry("capabilities", "3")
                .containsEntry("disabledSources", "1")
                .containsEntry("incompatibleSources", "1")
                .containsEntry("unhealthySources", "1");
    }

    private static DefaultGovernanceQueryService serviceWith(ExtensionGovernanceCatalog extensionCatalog) {
        return new DefaultGovernanceQueryService(
                List::of,
                new ToolRegistry() {
                    @Override
                    public List<io.github.pi_java.agent.domain.tool.ToolDescriptor> listTools() {
                        return List.of();
                    }

                    @Override
                    public Optional<ToolResolution> resolve(String toolId) {
                        return Optional.empty();
                    }
                },
                extensionCatalog,
                new McpGovernanceCatalog.EmptyMcpGovernanceCatalog(),
                new EmptyPluginGovernanceCatalog(),
                Optional.empty(),
                Optional.empty(),
                Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC));
    }

    private static ExtensionGovernanceCatalog catalogWithFailedAndDisabledSource() {
        ExtensionCapabilityStatus failedCapability = new ExtensionCapabilityStatus(
                "capability-1", "TOOL_PROVIDER", "FAILED", "1.0.0", "source-1", false,
                "INCOMPATIBLE", "UNHEALTHY", Map.of("secret", "<redacted>", "mutation", "disabled"));
        ExtensionCapabilityStatus healthyCapability = new ExtensionCapabilityStatus(
                "capability-2", "MODEL_PROVIDER", "AVAILABLE", "1.0.0", "source-1", true,
                "COMPATIBLE", "HEALTHY", Map.of("mutation", "disabled"));
        ExtensionSourceStatus failedSource = new ExtensionSourceStatus(
                "source-1", "Broken SPI Source", "1.0.0", "JAVA_SPI", "FAILED", false,
                "INCOMPATIBLE", "UNHEALTHY", "<redacted>", List.of(failedCapability, healthyCapability));
        ExtensionSourceStatus healthySource = new ExtensionSourceStatus(
                "source-2", "Spring Source", "1.0.0", "SPRING_BEAN", "ACTIVE", true,
                "COMPATIBLE", "HEALTHY", "", List.of(new ExtensionCapabilityStatus(
                "capability-3", "MEMORY_PROVIDER", "AVAILABLE", "1.0.0", "source-2", true,
                "COMPATIBLE", "HEALTHY", Map.of("mutation", "disabled"))));
        return () -> List.of(failedSource, healthySource);
    }
}
