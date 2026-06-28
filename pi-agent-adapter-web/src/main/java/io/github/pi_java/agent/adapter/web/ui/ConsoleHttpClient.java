package io.github.pi_java.agent.adapter.web.ui;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
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
import io.github.pi_java.agent.client.approval.ApprovalDecisionRequest;
import io.github.pi_java.agent.client.approval.ApprovalDecisionResponse;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Vaadin-side public REST boundary for the Agent Console.
 *
 * <p>This helper intentionally exposes relative {@code /api/**} paths and public {@code pi-agent-client}
 * DTO type anchors only. Downstream views can wire transport execution around these paths without injecting App,
 * Domain, runtime, or persistence objects into Vaadin components.</p>
 */
public class ConsoleHttpClient {

    public String createSessionPath() {
        return "/api/sessions";
    }

    public Class<CreateSessionRequest> createSessionRequestType() {
        return CreateSessionRequest.class;
    }

    public Class<SessionResponse> sessionResponseType() {
        return SessionResponse.class;
    }

    public String sessionPath(String sessionId) {
        return "/api/sessions/" + segment(sessionId);
    }

    public String sessionHistoryPath(String sessionId) {
        return sessionPath(sessionId) + "/history";
    }

    public Class<SessionHistoryResponse> sessionHistoryResponseType() {
        return SessionHistoryResponse.class;
    }

    public String recentSessionsPath(int limit, String cursor) {
        String path = "/api/sessions/recent?limit=" + Math.max(1, limit);
        return cursor == null || cursor.isBlank() ? path : path + "&cursor=" + segment(cursor);
    }

    @SuppressWarnings("unchecked")
    public Class<PageResponse<SessionSummaryDto>> recentSessionsResponseType() {
        return (Class<PageResponse<SessionSummaryDto>>) (Class<?>) PageResponse.class;
    }

    public String sessionTranscriptPath(String sessionId, int limit, String cursor) {
        String path = sessionPath(sessionId) + "/transcript?limit=" + Math.max(1, limit);
        return cursor == null || cursor.isBlank() ? path : path + "&cursor=" + segment(cursor);
    }

    public Class<ConversationTranscriptResponse> sessionTranscriptResponseType() {
        return ConversationTranscriptResponse.class;
    }

    public String createRunPath(String sessionId) {
        return sessionPath(sessionId) + "/runs";
    }

    public Class<CreateRunRequest> createRunRequestType() {
        return CreateRunRequest.class;
    }

    public Class<RunResponse> runResponseType() {
        return RunResponse.class;
    }

    public String runPath(String sessionId, String runId) {
        return createRunPath(sessionId) + "/" + segment(runId);
    }

    public Class<RunDetailResponse> runDetailResponseType() {
        return RunDetailResponse.class;
    }

    public String runStatusPath(String sessionId, String runId) {
        return runPath(sessionId, runId) + "/status";
    }

    public Class<RunStatusResponse> runStatusResponseType() {
        return RunStatusResponse.class;
    }

    public String runEventsPath(String sessionId, String runId, long afterSequence) {
        return runPath(sessionId, runId) + "/events?afterSequence=" + Math.max(0, afterSequence) + "&limit=500";
    }

    public Class<EventHistoryResponse> eventHistoryResponseType() {
        return EventHistoryResponse.class;
    }

    public String cancelRunPath(String sessionId, String runId) {
        return runPath(sessionId, runId) + "/cancel";
    }

    public Class<CancelRunRequest> cancelRunRequestType() {
        return CancelRunRequest.class;
    }

    public String agentCatalogPath() {
        return "/api/agents";
    }

    public Class<AgentCatalogResponse> agentCatalogResponseType() {
        return AgentCatalogResponse.class;
    }

    public String toolCatalogPath() {
        return "/api/tools";
    }

    public Class<ToolCatalogResponse> toolCatalogResponseType() {
        return ToolCatalogResponse.class;
    }

    public String approvalsPath(String sessionId, String runId) {
        return runPath(sessionId, runId) + "/approvals";
    }

    @SuppressWarnings("unchecked")
    public Class<List<ApprovalSummaryDto>> approvalsResponseType() {
        return (Class<List<ApprovalSummaryDto>>) (Class<?>) List.class;
    }

    public String approvalDecisionPath(String sessionId, String runId, String approvalId) {
        return approvalsPath(sessionId, runId) + "/" + segment(approvalId) + "/decision";
    }

    public Class<ApprovalDecisionRequest> approvalDecisionRequestType() {
        return ApprovalDecisionRequest.class;
    }

    public Class<ApprovalDecisionResponse> approvalDecisionResponseType() {
        return ApprovalDecisionResponse.class;
    }

    public String adminGovernanceOverviewPath() {
        return "/api/admin/governance/overview";
    }

    public Class<GovernanceOverviewResponse> adminGovernanceOverviewResponseType() {
        return GovernanceOverviewResponse.class;
    }

    public String adminGovernanceOperationsPath() {
        return "/api/admin/governance/operations";
    }

    public Class<OperationsSummaryResponse> adminGovernanceOperationsResponseType() {
        return OperationsSummaryResponse.class;
    }

    public String adminExtensionGovernancePath() {
        return "/api/admin/governance/extensions";
    }

    public Class<ExtensionGovernanceResponse> adminExtensionGovernanceResponseType() {
        return ExtensionGovernanceResponse.class;
    }

    public String adminMcpGovernancePath() {
        return "/api/admin/governance/mcp";
    }

    public Class<McpGovernanceResponse> adminMcpGovernanceResponseType() {
        return McpGovernanceResponse.class;
    }

    public String adminMcpRefreshPath() {
        return "/api/admin/governance/mcp/refresh";
    }

    public Class<McpRefreshResponse> adminMcpRefreshResponseType() {
        return McpRefreshResponse.class;
    }

    public String adminPluginGovernancePath() {
        return "/api/admin/governance/plugins";
    }

    public Class<PluginGovernanceResponse> adminPluginGovernanceResponseType() {
        return PluginGovernanceResponse.class;
    }

    public String adminPluginRefreshPath() {
        return adminPluginGovernancePath() + "/refresh";
    }

    public Class<PluginMutationResponse> adminPluginRefreshResponseType() {
        return PluginMutationResponse.class;
    }

    public String adminPluginDisablePath(String pluginId) {
        return adminPluginGovernancePath() + "/" + segment(pluginId) + "/disable";
    }

    public String adminPluginQuarantinePath(String pluginId) {
        return adminPluginGovernancePath() + "/" + segment(pluginId) + "/quarantine";
    }

    public Class<PluginMutationRequest> adminPluginMutationRequestType() {
        return PluginMutationRequest.class;
    }

    public Class<PluginMutationResponse> adminPluginMutationResponseType() {
        return PluginMutationResponse.class;
    }

    public String adminPolicyDecisionsPath() {
        return "/api/admin/governance/policy-decisions";
    }

    @SuppressWarnings("unchecked")
    public Class<List<PolicyDecisionSummaryDto>> adminPolicyDecisionsResponseType() {
        return (Class<List<PolicyDecisionSummaryDto>>) (Class<?>) List.class;
    }

    public String adminAuditsPath() {
        return "/api/admin/governance/audits";
    }

    @SuppressWarnings("unchecked")
    public Class<List<AuditSummaryDto>> adminAuditsResponseType() {
        return (Class<List<AuditSummaryDto>>) (Class<?>) List.class;
    }

    private static String segment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Path segment must not be blank");
        }
        return URLEncoder.encode(raw.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
