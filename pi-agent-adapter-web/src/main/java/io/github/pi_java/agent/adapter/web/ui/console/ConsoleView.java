package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import java.util.List;
import java.util.Map;

/** Chat-first user Console route backed by public REST/SSE helper boundaries. */
@Route(value = "console", layout = PiResponsiveShell.class)
@PageTitle("Pi Agent Console")
public class ConsoleView extends Div {

    private static final String DEFAULT_AGENT_ID = "cloud-general-agent";
    private static final String PENDING_SESSION_ID = "pending-session";
    private static final String PENDING_RUN_ID = "pending-run";
    private static final String CHAT_PANEL_SELECTOR_CONTRACT = "data-console-panel=chat";
    private static final String AGENTS_TARGET_SELECTOR_CONTRACT = "data-console-target=agents";

    private final ConsoleHttpClient httpClient;
    private final EventStreamClient eventStreamClient;
    private final SessionListPanel sessionListPanel;
    private final AgentCatalogPanel agentCatalogPanel;
    private final ChatEventStreamPanel chatPanel;
    private final RunContextPanel runContextPanel;
    private final Map<String, Div> consolePanels;
    private final Map<String, Button> panelControls;
    private String selectedAgentId = DEFAULT_AGENT_ID;
    private String selectedSessionId;
    private String activeRunId;
    private String activeConsolePanel = "chat";

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
        Div switcher = createPanelSwitcher();
        Div agentsPanel = panelWrapper("agents", agentCatalogPanel);
        Div sessionsPanel = panelWrapper("sessions", sessionListPanel);
        Div chatPanelWrapper = panelWrapper("chat", chatPanel);
        Div runContextPanelWrapper = panelWrapper("run-context", runContextPanel);
        this.consolePanels = Map.of(
                "chat", chatPanelWrapper,
                "agents", agentsPanel,
                "sessions", sessionsPanel,
                "run-context", runContextPanelWrapper);
        this.panelControls = Map.of(
                "chat", panelControl(switcher, "chat"),
                "agents", panelControl(switcher, "agents"),
                "sessions", panelControl(switcher, "sessions"),
                "run-context", panelControl(switcher, "run-context"));
        addClassName("pi-console-workbench");
        getElement().setAttribute("data-route", "console");
        getElement().setAttribute("data-layout", "three-column-workbench");
        getElement().setAttribute("data-mobile-critical", "true");
        sessionListPanel.add(agentCatalogPanel);
        add(switcher, sessionsPanel, chatPanelWrapper, runContextPanelWrapper, agentsPanel);
        applyPanelState();
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

    public void showConsolePanel(String target) {
        String normalized = requireText(target, "target");
        if (!consolePanels.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown console panel: " + target);
        }
        activeConsolePanel = normalized;
        applyPanelState();
    }

    public String activeConsolePanel() {
        return activeConsolePanel;
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

    private Div createPanelSwitcher() {
        Div switcher = new Div();
        switcher.addClassName("pi-console-panel-switcher");
        switcher.getElement().setAttribute("data-role", "console-panel-switcher");
        switcher.add(panelButton("Chat", "chat"));
        switcher.add(panelButton("Agents", "agents"));
        switcher.add(panelButton("Sessions", "sessions"));
        switcher.add(panelButton("Run", "run-context"));
        return switcher;
    }

    private Button panelButton(String label, String target) {
        Button button = new Button(label, event -> showConsolePanel(target));
        button.getElement().setAttribute("data-action", "show-console-panel");
        button.getElement().setAttribute("data-console-target", target);
        button.getElement().setAttribute("aria-pressed", "false");
        return button;
    }

    private static Button panelControl(Div switcher, String target) {
        return switcher.getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> target.equals(button.getElement().getAttribute("data-console-target")))
                .findFirst()
                .orElseThrow();
    }

    private static Div panelWrapper(String panel, Div content) {
        Div wrapper = new Div(content);
        wrapper.addClassName("pi-console-panel");
        wrapper.addClassName("pi-console-panel-" + panel);
        wrapper.getElement().setAttribute("data-console-panel", panel);
        wrapper.getElement().setAttribute("data-console-panel-active", "false");
        return wrapper;
    }

    private void applyPanelState() {
        consolePanels.forEach((panel, wrapper) -> wrapper.getElement()
                .setAttribute("data-console-panel-active", Boolean.toString(panel.equals(activeConsolePanel))));
        panelControls.forEach((panel, button) -> button.getElement()
                .setAttribute("aria-pressed", Boolean.toString(panel.equals(activeConsolePanel))));
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
