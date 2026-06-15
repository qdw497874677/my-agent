package io.github.pi_java.agent.client.admin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GovernanceOverviewResponse(
        GovernanceStatusDto runtime,
        GovernanceStatusDto providers,
        GovernanceStatusDto toolRegistry,
        GovernanceStatusDto extensions,
        GovernanceStatusDto mcp,
        GovernanceStatusDto plugins,
        List<PolicyDecisionSummaryDto> policyDecisions,
        List<AuditSummaryDto> audits,
        Instant generatedAt
) {
    public GovernanceOverviewResponse {
        runtime = Objects.requireNonNull(runtime, "runtime must not be null");
        providers = Objects.requireNonNull(providers, "providers must not be null");
        toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
        extensions = Objects.requireNonNull(extensions, "extensions must not be null");
        mcp = Objects.requireNonNull(mcp, "mcp must not be null");
        plugins = Objects.requireNonNull(plugins, "plugins must not be null");
        policyDecisions = List.copyOf(Objects.requireNonNull(policyDecisions, "policyDecisions must not be null"));
        audits = List.copyOf(Objects.requireNonNull(audits, "audits must not be null"));
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    }
}
