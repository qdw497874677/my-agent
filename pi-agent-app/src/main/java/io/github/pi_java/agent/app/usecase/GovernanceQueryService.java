package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginMutationRequest;
import io.github.pi_java.agent.client.admin.PluginMutationResponse;
import java.util.List;

public interface GovernanceQueryService {

    GovernanceOverviewResponse overview(RequestContext context);

    ExtensionGovernanceResponse extensions(RequestContext context);

    McpGovernanceResponse mcp(RequestContext context);

    McpRefreshResponse refreshMcp(RequestContext context);

    PluginGovernanceResponse plugins(RequestContext context);

    PluginMutationResponse refreshPlugins(RequestContext context);

    PluginMutationResponse disablePlugin(RequestContext context, String pluginId, PluginMutationRequest request);

    PluginMutationResponse quarantinePlugin(RequestContext context, String pluginId, PluginMutationRequest request);

    OperationsSummaryResponse operations(RequestContext context);

    List<PolicyDecisionSummaryDto> policyDecisions(RequestContext context);

    List<AuditSummaryDto> audits(RequestContext context);
}
