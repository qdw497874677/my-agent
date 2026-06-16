package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpRefreshResponse;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import java.util.List;

public interface GovernanceQueryService {

    GovernanceOverviewResponse overview(RequestContext context);

    ExtensionGovernanceResponse extensions(RequestContext context);

    McpGovernanceResponse mcp(RequestContext context);

    McpRefreshResponse refreshMcp(RequestContext context);

    List<PolicyDecisionSummaryDto> policyDecisions(RequestContext context);

    List<AuditSummaryDto> audits(RequestContext context);
}
