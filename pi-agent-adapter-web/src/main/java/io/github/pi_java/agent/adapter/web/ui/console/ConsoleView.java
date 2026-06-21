package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import java.util.List;
import java.util.Map;

/** Chat-first user Console route backed by public REST/SSE helper boundaries. */
@Route("console")
@PageTitle("Pi Agent Console")
public class ConsoleView extends Div {

    private static final String DEFAULT_AGENT_ID = "cloud-general-agent";
    private static final String PENDING_SESSION_ID = "pending-session";
    private static final String PENDING_RUN_ID = "pending-run";

    private final ConsoleHttpClient httpClient;
    private final EventStreamClient eventStreamClient;
    private final SessionListPanel sessionListPanel;
    private final AgentCatalogPanel agentCatalogPanel;
    private final ChatEventStreamPanel chatPanel;
    private final RunContextPanel runContextPanel;
    private String selectedAgentId = DEFAULT_AGENT_ID;
    private String selectedSessionId;
    private String activeRunId;

    public ConsoleView() {
        this(new ConsoleHttpClient(), new EventStreamClient());
    }

    public ConsoleView(ConsoleHttpClient httpClient, EventStreamClient eventStreamClient) {
        this.httpClient = httpClient;
        this.eventStreamClient = eventStreamClient;
        this.sessionListPanel = new SessionListPanel();
        this.agentCatalogPanel = new AgentCatalogPanel(httpClient);
        this.chatPanel = new ChatEventStreamPanel();
        this.runContextPanel = new RunContextPanel();
        addClassName("pi-console-workbench");
        getElement().setAttribute("data-route", "console");
        getElement().setAttribute("data-layout", "three-column-workbench");
        getElement().setAttribute("data-mobile-critical", "true");
        sessionListPanel.add(agentCatalogPanel);
        add(sessionListPanel, chatPanel, runContextPanel);
    }

    public RunSubmissionPlan planChatSubmission(String text) {
        String message = requireText(text, "text");
        chatPanel.appendUserMessage(message);
        String sessionId = selectedSessionId == null ? PENDING_SESSION_ID : selectedSessionId;
        String runId = PENDING_RUN_ID;
        activeRunId = runId;
        CreateRunRequest request = new CreateRunRequest(
                selectedAgentId,
                "chat",
                Map.of("text", message),
                null,
                Map.of("source", "vaadin-console"));
        EventStreamClient.ConnectionSpec streamSpec = eventStreamClient.runEventStream(sessionId, runId, 0);
        runContextPanel.showRunning(sessionId, runId);
        return new RunSubmissionPlan(
                selectedSessionId == null ? httpClient.createSessionPath() : null,
                sessionId,
                httpClient.createRunPath(sessionId),
                request,
                streamSpec);
    }

    public SessionSelectionPlan selectSession(String sessionId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        sessionListPanel.selectSession(selectedSessionId);
        return new SessionSelectionPlan(selectedSessionId, httpClient.sessionHistoryPath(selectedSessionId));
    }

    public void markRunRunning(String sessionId, String runId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        activeRunId = requireText(runId, "runId");
        runContextPanel.showRunning(selectedSessionId, activeRunId);
    }

    public CancelPlan planCancelRunningRun(String reason) {
        if (selectedSessionId == null || activeRunId == null) {
            throw new IllegalStateException("No active run to cancel");
        }
        runContextPanel.showCancelling();
        return new CancelPlan(httpClient.cancelRunPath(selectedSessionId, activeRunId), new CancelRunRequest(reason));
    }

    public void applyRunStatus(String status, boolean terminal) {
        runContextPanel.showStatus(status, terminal);
    }

    public AgentCatalogPlan agentCatalogPlan() {
        return new AgentCatalogPlan(httpClient.agentCatalogPath(), httpClient.agentCatalogResponseType().getName());
    }

    public List<String> columnOrder() {
        return List.of("sessions", "chat-event-stream", "run-context");
    }

    public String selectedAgentId() {
        return selectedAgentId;
    }

    public SessionListPanel sessionListPanel() {
        return sessionListPanel;
    }

    public AgentCatalogPanel agentCatalogPanel() {
        return agentCatalogPanel;
    }

    public ChatEventStreamPanel chatPanel() {
        return chatPanel;
    }

    public RunContextPanel runContextPanel() {
        return runContextPanel;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public record AgentCatalogPlan(String path, String responseType) {
    }

    public record SessionSelectionPlan(String sessionId, String historyPath) {
    }

    public record RunSubmissionPlan(
            String createSessionPath,
            String sessionId,
            String createRunPath,
            CreateRunRequest request,
            EventStreamClient.ConnectionSpec streamSpec) {
    }

    public record CancelPlan(String cancelPath, CancelRunRequest request) {
    }
}
