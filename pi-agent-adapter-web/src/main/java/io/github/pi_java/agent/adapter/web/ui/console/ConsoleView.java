package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.ApprovalCommandService;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/** Chat-first user Console route backed by public REST/SSE helper boundaries. */
@Route(value = "console", layout = PiResponsiveShell.class)
@PageTitle("Pi Agent Console")
public class ConsoleView extends Div {

    private static final String DEFAULT_AGENT_ID = "cloud-general-agent";
    private static final String PENDING_SESSION_ID = "pending-session";
    private static final String PENDING_RUN_ID = "pending-run";
    private static final String CHAT_PANEL_SELECTOR_CONTRACT = "data-console-panel=chat";
    private static final String AGENTS_TARGET_SELECTOR_CONTRACT = "data-console-target=agents";
    private static final String SELECT_SESSION_RETURN_CONTRACT = "showConsolePanel(\"chat\")";

    private final ConsoleHttpClient httpClient;
    private final EventStreamClient eventStreamClient;
    private final AgentCatalogQueryService agentCatalogQueryService;
    private final ConsoleRunExecutionBridge executionBridge;
    private final RunEventRenderer runEventRenderer;
    private final SessionListPanel sessionListPanel;
    private final AgentCatalogPanel agentCatalogPanel;
    private final ChatEventStreamPanel chatPanel;
    private final RunContextPanel runContextPanel;
    private final Map<String, Div> consolePanels;
    private final Map<String, Button> panelControls;
    private String selectedAgentId = DEFAULT_AGENT_ID;
    private String selectedSessionId;
    private String activeRunId;
    private long activeRunNextAfterSequence;
    private String activeConsolePanel = "chat";

    public ConsoleView() {
        this(new ConsoleHttpClient(), new EventStreamClient(), new DefaultAgentCatalogQueryService(), new DemoConsoleRunExecutionBridge(), new RunEventRenderer());
    }

    @Autowired
    public ConsoleView(
            AgentCatalogQueryService agentCatalogQueryService,
            SessionCommandService sessionCommandService,
            RunCommandService runCommandService,
            RunQueryService runQueryService,
            ApprovalCommandService approvalCommandService) {
        this(
                new ConsoleHttpClient(),
                new EventStreamClient(),
                agentCatalogQueryService,
                new AppConsoleRunExecutionBridge(sessionCommandService, runCommandService, runQueryService),
                new RunEventRenderer(new ConsoleHttpClient(), new AppApprovalDecisionHandler(approvalCommandService)));
    }

    public ConsoleView(AgentCatalogQueryService agentCatalogQueryService) {
        this(new ConsoleHttpClient(), new EventStreamClient(), agentCatalogQueryService, new DemoConsoleRunExecutionBridge(), new RunEventRenderer());
    }

    public ConsoleView(ConsoleHttpClient httpClient, EventStreamClient eventStreamClient) {
        this(httpClient, eventStreamClient, new DefaultAgentCatalogQueryService(), new DemoConsoleRunExecutionBridge(), new RunEventRenderer(httpClient));
    }

    public ConsoleView(ConsoleHttpClient httpClient, EventStreamClient eventStreamClient, AgentCatalogQueryService agentCatalogQueryService) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, new DemoConsoleRunExecutionBridge(), new RunEventRenderer(httpClient));
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, new RunEventRenderer(httpClient));
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer) {
        this.httpClient = httpClient;
        this.eventStreamClient = eventStreamClient;
        this.agentCatalogQueryService = agentCatalogQueryService;
        this.executionBridge = executionBridge;
        this.runEventRenderer = runEventRenderer;
        this.sessionListPanel = new SessionListPanel();
        this.agentCatalogPanel = new AgentCatalogPanel(httpClient);
        this.chatPanel = new ChatEventStreamPanel();
        this.runContextPanel = new RunContextPanel();
        wireActionHandlers();
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
        add(switcher, sessionsPanel, chatPanelWrapper, runContextPanelWrapper, agentsPanel);
        addAttachListener(event -> {
            event.getUI().setPollInterval(750);
            event.getUI().addPollListener(poll -> refreshActiveRunEvents());
        });
        loadInitialAgentCatalog();
        applyPanelState();
    }

    private void wireActionHandlers() {
        agentCatalogPanel.setAgentActionHandler(this::handleAgentAction);
        sessionListPanel.setSessionActivationHandler(this::selectSession);
        chatPanel.setSubmitHandler(this::planChatSubmission);
        Runnable cancel = () -> handleCancelRunningRun("mobile user requested cancellation");
        chatPanel.setCancelHandler(cancel);
        runContextPanel.setCancelHandler(cancel);
    }

    private void handleAgentAction(String agentId, String actionId) {
        selectedAgentId = requireText(agentId, "agentId");
        showConsolePanel("chat");
    }

    private void loadInitialAgentCatalog() {
        agentCatalogPanel.showCatalog(agentCatalogQueryService.listAgents(consoleRequestContext()));
    }

    static RequestContext consoleRequestContext() {
        return new RequestContext(
                new SecurityPrincipalContext("console", "vaadin-console", Set.of("ROLE_USER")),
                new CorrelationContext("vaadin-console", "vaadin-console", null));
    }

    public RunSubmissionPlan planChatSubmission(String text) {
        String message = requireText(text, "text");
        chatPanel.appendUserMessage(message);
        boolean needsSession = selectedSessionId == null;
        String sessionId = needsSession ? executionBridge.createSession().sessionId() : selectedSessionId;
        selectedSessionId = sessionId;
        CreateRunRequest request = new CreateRunRequest(
                selectedAgentId,
                "chat",
                Map.of("text", message),
                null,
                Map.of("source", "vaadin-console"));
        RunResponse run = executionBridge.createRun(sessionId, request);
        String runId = run.runId();
        activeRunId = runId;
        activeRunNextAfterSequence = 0;
        EventStreamClient.ConnectionSpec streamSpec = eventStreamClient.runEventStream(sessionId, runId, 0);
        sessionListPanel.showSession(sessionId, sessionTitle(message), run.status(), latest(run.createdAt(), run.updatedAt()));
        sessionListPanel.selectSession(sessionId);
        runContextPanel.showRunning(sessionId, runId);
        chatPanel.showComposerRunStatus("Run status: " + run.status(), isCancellable(run.status()));
        appendRunEvents(executionBridge.listEvents(sessionId, runId, 0));
        return new RunSubmissionPlan(
                needsSession ? httpClient.createSessionPath() : null,
                sessionId,
                httpClient.createRunPath(sessionId),
                request,
                streamSpec);
    }

    public SessionSelectionPlan selectSession(String sessionId) {
        selectedSessionId = requireText(sessionId, "sessionId");
        sessionListPanel.selectSession(selectedSessionId);
        showConsolePanel("chat");
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
        activeRunNextAfterSequence = 0;
        runContextPanel.showRunning(selectedSessionId, activeRunId);
        chatPanel.showComposerRunStatus("Running run " + activeRunId + " in session " + selectedSessionId, true);
    }

    public int refreshActiveRunEvents() {
        if (selectedSessionId == null || activeRunId == null) {
            return 0;
        }
        EventHistoryResponse history = executionBridge.listEvents(selectedSessionId, activeRunId, activeRunNextAfterSequence);
        return appendRunEvents(history);
    }

    public CancelPlan planCancelRunningRun(String reason) {
        if (selectedSessionId == null || activeRunId == null) {
            throw new IllegalStateException("No active run to cancel");
        }
        runContextPanel.showCancelling();
        chatPanel.showComposerCancelling();
        CancelRunRequest request = new CancelRunRequest(reason);
        RunStatusResponse response = executionBridge.cancelRun(selectedSessionId, activeRunId, request);
        applyRunStatus(response.status(), response.terminal());
        activeRunId = response.terminal() ? null : response.runId();
        return new CancelPlan(httpClient.cancelRunPath(response.sessionId(), response.runId()), request);
    }

    void handleCancelRunningRun(String reason) {
        try {
            planCancelRunningRun(reason);
        } catch (IllegalStateException noActiveRun) {
            runContextPanel.showStatus("no active run", true);
            chatPanel.showComposerRunStatus("No active run to cancel", false);
        }
    }

    public void applyRunStatus(String status, boolean terminal) {
        runContextPanel.showStatus(status, terminal);
        chatPanel.showComposerRunStatus("Run status: " + requireText(status, "status"), !terminal && isCancellable(status));
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

    private static boolean isCancellable(String runStatus) {
        return runStatus != null
                && (runStatus.equalsIgnoreCase("running")
                || runStatus.equalsIgnoreCase("queued")
                || runStatus.equalsIgnoreCase("cancelling"));
    }

    private static String sessionTitle(String message) {
        String normalized = requireText(message, "message").replaceAll("\\s+", " ").trim();
        return normalized.length() <= 72 ? normalized : normalized.substring(0, 69) + "…";
    }

    private static Instant latest(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.isAfter(first) ? second : first;
    }

    private int appendRunEvents(EventHistoryResponse history) {
        if (history == null || history.events() == null) {
            return 0;
        }
        int appended = 0;
        for (RunEventDto event : history.events()) {
            if (event.sequence() <= activeRunNextAfterSequence) {
                continue;
            }
            RunEventRenderer.RenderedEvent rendered = runEventRenderer.render(event);
            chatPanel.appendEvent(rendered);
            activeRunNextAfterSequence = Math.max(activeRunNextAfterSequence, event.sequence());
            appended++;
            boolean statusApplied = false;
            if (event.type() != null && event.type().toLowerCase().contains("status") && event.payload() != null) {
                Object status = event.payload().get("status");
                if (status != null) {
                    applyRunStatus(String.valueOf(status), rendered.terminal());
                    statusApplied = true;
                }
            }
            if (rendered.terminal() && !statusApplied) {
                applyRunStatus("terminal", true);
            }
        }
        if (history.nextAfterSequence() > activeRunNextAfterSequence) {
            activeRunNextAfterSequence = history.nextAfterSequence();
        }
        return appended;
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

    static final class DemoConsoleRunExecutionBridge implements ConsoleRunExecutionBridge {

        private int runCounter;

        @Override
        public SessionResponse createSession() {
            return new SessionResponse(
                    "tenant", "user", "session-mobile-1", "workspace", null, "ACTIVE", Instant.now(), Instant.now(), Map.of("source", "vaadin-console"));
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            runCounter++;
            String runId = runCounter == 1 ? "run-mobile-1" : "run-mobile-" + runCounter;
            return new RunResponse("tenant", "user", sessionId, runId, "workspace", "QUEUED", "trace", "correlation", Instant.now(), Instant.now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            List<RunEventDto> events = new ArrayList<>();
            events.add(event(sessionId, runId, 1, "run.status", Map.of("status", "RUNNING")));
            events.add(event(sessionId, runId, 2, "model.delta", Map.of("text", "model reply")));
            return new EventHistoryResponse(sessionId, runId, events, afterSequence, afterSequence + events.size(), false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "cancelled", true, Instant.now(), "trace", "correlation");
        }

        private static RunEventDto event(String sessionId, String runId, long sequence, String type, Map<String, Object> payload) {
            return new RunEventDto(
                    "event-" + sequence,
                    "tenant",
                    "user",
                    sessionId,
                    runId,
                    null,
                    "workspace",
                    sequence,
                    Instant.now(),
                    type,
                    "trace",
                    "correlation",
                    null,
                    "USER",
                    null,
                    type,
                    1,
                    payload);
        }
    }
}
