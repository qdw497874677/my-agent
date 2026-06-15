package io.github.pi_java.agent.adapter.web.ui;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import io.github.pi_java.agent.client.tool.ToolCatalogResponse;
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

    public String toolCatalogPath() {
        return "/api/tools";
    }

    public Class<ToolCatalogResponse> toolCatalogResponseType() {
        return ToolCatalogResponse.class;
    }

    private static String segment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Path segment must not be blank");
        }
        return URLEncoder.encode(raw.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
