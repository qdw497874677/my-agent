package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigController;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger;
import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.ApprovalCommandService;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Map;
import java.util.ResourceBundle;
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
    private static final String DEFAULT_ASSISTANT_STEP_ID = "step-1";

    private final ConsoleHttpClient httpClient;
    private final EventStreamClient eventStreamClient;
    private final AgentCatalogQueryService agentCatalogQueryService;
    private final ConsoleRunExecutionBridge executionBridge;
    private final RunEventRenderer runEventRenderer;
    private final ConsoleLiveRunEventSubscriber liveRunEventSubscriber;
    private final ConversationEventReducer conversationEventReducer = new ConversationEventReducer();
    private final SessionListPanel sessionListPanel;
    private final AgentCatalogPanel agentCatalogPanel;
    private final ChatEventStreamPanel chatPanel;
    private final RunContextPanel runContextPanel;
    private final Div activeSessionBanner = new Div();
    private final Span activeSessionLabel = new Span();
    private final Button newConversationAction = new Button();
    private final Map<String, Div> consolePanels;
    private final Map<String, Button> panelControls;
    private final Map<String, String> recentSessionTitles = new java.util.LinkedHashMap<>();
    private ProviderConfigStore providerConfigStore;
    private ProviderConfigController providerConfigController;
    private ComboBox<String> modelSelector;
    private Span providerStatus;
    private Span modelRefreshStatus;
    private Span modelSelectionScopeStatus;
    private Span fallbackModeStatus;
    private final boolean explicitLocalFallbackMode;
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
            ConversationQueryService conversationQueryService,
            ApprovalCommandService approvalCommandService,
            RunActivationTrigger runActivationTrigger,
            RunDispatcher runDispatcher,
            ProviderConfigStore providerConfigStore,
            ProviderConfigController providerConfigController) {
        this(
                new ConsoleHttpClient(),
                new EventStreamClient(),
                agentCatalogQueryService,
                new AppConsoleRunExecutionBridge(sessionCommandService, runCommandService, runQueryService, conversationQueryService, runActivationTrigger, runDispatcher),
                new RunEventRenderer(new ConsoleHttpClient(), new AppApprovalDecisionHandler(approvalCommandService)),
                providerConfigStore,
                providerConfigController);
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
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, runEventRenderer, null, null);
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer,
            ConsoleLiveRunEventSubscriber liveRunEventSubscriber) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, runEventRenderer, null, null, liveRunEventSubscriber);
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer,
            ProviderConfigStore providerConfigStore,
            ProviderConfigController providerConfigController) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, runEventRenderer,
                providerConfigStore, providerConfigController, null, false);
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer,
            ProviderConfigStore providerConfigStore,
            ProviderConfigController providerConfigController,
            boolean explicitLocalFallbackMode) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, runEventRenderer,
                providerConfigStore, providerConfigController, null, explicitLocalFallbackMode);
    }

    public ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer,
            ProviderConfigStore providerConfigStore,
            ProviderConfigController providerConfigController,
            ConsoleLiveRunEventSubscriber liveRunEventSubscriber) {
        this(httpClient, eventStreamClient, agentCatalogQueryService, executionBridge, runEventRenderer,
                providerConfigStore, providerConfigController, liveRunEventSubscriber, false);
    }

    private ConsoleView(
            ConsoleHttpClient httpClient,
            EventStreamClient eventStreamClient,
            AgentCatalogQueryService agentCatalogQueryService,
            ConsoleRunExecutionBridge executionBridge,
            RunEventRenderer runEventRenderer,
            ProviderConfigStore providerConfigStore,
            ProviderConfigController providerConfigController,
            ConsoleLiveRunEventSubscriber liveRunEventSubscriber,
            boolean explicitLocalFallbackMode) {
        this.httpClient = httpClient;
        this.eventStreamClient = eventStreamClient;
        this.agentCatalogQueryService = agentCatalogQueryService;
        this.executionBridge = executionBridge;
        this.runEventRenderer = runEventRenderer;
        this.liveRunEventSubscriber = liveRunEventSubscriber;
        this.explicitLocalFallbackMode = explicitLocalFallbackMode;
        this.providerConfigStore = providerConfigStore;
        this.providerConfigController = providerConfigController;
        this.sessionListPanel = new SessionListPanel();
        this.agentCatalogPanel = new AgentCatalogPanel(httpClient);
        this.chatPanel = new ChatEventStreamPanel();
        this.runContextPanel = new RunContextPanel();
        wireActionHandlers();
        Div switcher = createPanelSwitcher();
        Div agentsPanel = panelWrapper("agents", agentCatalogPanel);
        Div sessionsPanel = panelWrapper("sessions", sessionListPanel);
        configureActiveSessionBanner();
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
        addClassName("pi-console-home");
        getElement().setAttribute("data-route", "console");
        getElement().setAttribute("data-layout", "chat-home");
        getElement().setAttribute("data-mobile-critical", "true");
        getElement().setAttribute("data-stream-mode", liveStreamingAvailable() ? "push" : "polling-fallback");
        Div hero = createConversationHero();
        Div modelBar = createModelBar();
        Div advancedPanels = new Div(sessionsPanel, runContextPanelWrapper, agentsPanel);
        advancedPanels.addClassName("pi-console-advanced-panels");
        advancedPanels.getElement().setAttribute("data-role", "advanced-console-panels");
        add(hero, modelBar, switcher, activeSessionBanner, chatPanelWrapper, advancedPanels);
        addAttachListener(event -> {
            event.getUI().setPollInterval(750);
            event.getUI().addPollListener(poll -> refreshActiveRunEvents());
            loadRecentSessionsForProof();
        });
        loadInitialAgentCatalog();
        applyPanelState();
    }

    private Div createConversationHero() {
        H1 title = new H1(getTranslation("console.home.title"));
        Paragraph subtitle = new Paragraph(getTranslation("console.home.subtitle"));
        Span badge = new Span(getTranslation("console.home.badge"));
        Div hero = new Div(badge, title, subtitle);
        hero.addClassName("pi-console-hero");
        hero.getElement().setAttribute("data-role", "conversation-hero");
        hero.getStyle().set("max-width", "820px");
        hero.getStyle().set("margin", "2.5rem auto 1rem");
        hero.getStyle().set("text-align", "center");
        badge.getStyle().set("display", "inline-block");
        badge.getStyle().set("padding", "0.25rem 0.7rem");
        badge.getStyle().set("border-radius", "999px");
        badge.getStyle().set("background", "var(--lumo-contrast-5pct)");
        badge.getStyle().set("color", "var(--lumo-secondary-text-color)");
        title.getStyle().set("margin", "0.8rem 0 0.5rem");
        title.getStyle().set("font-size", "clamp(2rem, 6vw, 4.5rem)");
        title.getStyle().set("letter-spacing", "-0.08em");
        subtitle.getStyle().set("margin", "0 auto");
        subtitle.getStyle().set("max-width", "620px");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("font-size", "1.05rem");
        return hero;
    }

    private void wireActionHandlers() {
        agentCatalogPanel.setAgentActionHandler(this::handleAgentAction);
        sessionListPanel.setSessionActivationHandler(this::selectSession);
        chatPanel.setSubmitHandler(this::planChatSubmission);
        Runnable cancel = () -> handleCancelRunningRun("mobile user requested cancellation");
        chatPanel.setCancelHandler(cancel);
        runContextPanel.setCancelHandler(cancel);
    }

    private void configureActiveSessionBanner() {
        activeSessionBanner.addClassName("pi-active-session-banner");
        activeSessionBanner.getElement().setAttribute("data-role", "active-session-banner");
        activeSessionBanner.getStyle().set("max-width", "820px");
        activeSessionBanner.getStyle().set("margin", "0 auto 0.75rem");
        activeSessionBanner.getStyle().set("padding", "0.55rem 0.8rem");
        activeSessionBanner.getStyle().set("border-radius", "999px");
        activeSessionBanner.getStyle().set("background", "var(--lumo-contrast-5pct)");
        activeSessionBanner.getStyle().set("display", "flex");
        activeSessionBanner.getStyle().set("align-items", "center");
        activeSessionBanner.getStyle().set("justify-content", "space-between");
        activeSessionBanner.getStyle().set("gap", "0.75rem");
        activeSessionLabel.getElement().setAttribute("data-role", "active-session-label");
        newConversationAction.setText(t("console.session.action.newConversation"));
        newConversationAction.getElement().setAttribute("data-action", "new-conversation");
        newConversationAction.addClickListener(event -> startNewConversation());
        activeSessionBanner.add(activeSessionLabel, newConversationAction);
        updateActiveSessionBanner(null);
    }

    private void updateActiveSessionBanner(String title) {
        boolean continued = selectedSessionId != null;
        activeSessionBanner.getElement().setAttribute("data-active-session-state", continued ? "continued" : "new");
        activeSessionLabel.setText(continued
                ? t("console.session.continueTitle", title == null || title.isBlank() ? selectedSessionId : title)
                : t("console.session.new"));
        newConversationAction.setVisible(continued);
    }

    public void startNewConversation() {
        selectedSessionId = null;
        activeRunId = null;
        activeRunNextAfterSequence = 0;
        sessionListPanel.clearSelection();
        chatPanel.replaceTranscript(List.of());
        runContextPanel.showStatus(t("console.session.new"), true);
        chatPanel.showComposerRunStatus(t("chat.noActiveRun"), false);
        updateActiveSessionBanner(null);
        showConsolePanel("chat");
    }

    private void initModelSelector() {
        if (providerConfigStore == null) return;
        ProviderConfig config = providerConfigStore.current();
        modelSelector.setItems(List.of());
        modelSelector.setValue(config.modelId());
    }

    private Div createModelBar() {
        modelSelector = new ComboBox<>();
        if (providerConfigStore != null) {
            modelSelector.setLabel(getTranslation("console.modelSelector.label"));
            modelSelector.setAllowCustomValue(true);
            modelSelector.setItems(List.of());
            modelSelector.setValue(providerConfigStore.current().modelId());
            modelSelector.setWidth("220px");
            modelSelector.getElement().setAttribute("data-role", "model-selector");

            Button refreshModels = new Button(getTranslation("console.modelSelector.refresh"));
            refreshModels.getElement().setAttribute("data-action", "refresh-models");
            refreshModels.addClickListener(event -> {
                if (providerConfigController == null) {
                    updateRefreshStatus("not_configured", t("console.modelSelector.refreshNotConfigured"));
                    return;
                }
                var response = providerConfigController.listModels();
                if (response.models() != null && !response.models().isEmpty()) {
                    modelSelector.setItems(response.models());
                }
                updateProviderStatus(response.ready(), response.providerId(), response.selectedModel());
                updateRefreshStatus(response.state(), localizedRefreshMessage(response));
            });

            modelSelector.addValueChangeListener(event -> {
                if (event.getValue() == null || event.getValue().isBlank()) return;
                ProviderConfig current = providerConfigStore.current();
                providerConfigStore.update(new ProviderConfig(
                        current.enabled(), current.baseUrl(), current.apiKey(),
                        event.getValue(), current.providerId(), current.completionsPath()));
                updateProviderStatus(providerConfigStore.current().isReady(), current.providerId(), event.getValue());
                updateModelSelectionScopeStatus(hasActiveRun());
            });

            providerStatus = new Span();
            providerStatus.getElement().setAttribute("data-role", "provider-status");
            ProviderConfig current = providerConfigStore.current();
            updateProviderStatus(current.isReady(), current.providerId(), current.modelId());

            modelRefreshStatus = new Span();
            modelRefreshStatus.getElement().setAttribute("data-role", "model-refresh-status");
            updateRefreshStatus("idle", t("console.modelSelector.refreshIdle"));

            modelSelectionScopeStatus = new Span();
            modelSelectionScopeStatus.getElement().setAttribute("data-role", "model-selection-scope");
            updateModelSelectionScopeStatus(false);

            fallbackModeStatus = new Span();
            fallbackModeStatus.getElement().setAttribute("data-role", "fallback-label");
            updateFallbackModeStatus();

            Div bar = new Div(modelSelector, refreshModels, providerStatus, modelRefreshStatus, modelSelectionScopeStatus, fallbackModeStatus);
            bar.addClassName("pi-console-model-bar");
            bar.getStyle().set("display", "flex");
            bar.getStyle().set("gap", "0.5rem");
            bar.getStyle().set("align-items", "flex-end");
            bar.getStyle().set("justify-content", "center");
            bar.getStyle().set("flex-wrap", "wrap");
            bar.getStyle().set("max-width", "820px");
            bar.getStyle().set("margin", "0 auto 1rem");
            bar.setWidthFull();
            return bar;
        }
        return new Div();
    }

    private void updateProviderStatus(boolean ready, String providerId, String selectedModel) {
        if (providerStatus == null) {
            return;
        }
        providerStatus.getElement().setAttribute("data-provider-ready", Boolean.toString(ready));
        providerStatus.setText(ready
                ? t("console.modelSelector.readyWithModel", display(providerId, "provider"), display(selectedModel, "model"))
                : t("console.modelSelector.notConfiguredAction"));
    }

    private void updateRefreshStatus(String state, String message) {
        if (modelRefreshStatus == null) {
            return;
        }
        String normalized = state == null || state.isBlank() ? "idle" : state.trim();
        modelRefreshStatus.getElement().setAttribute("data-refresh-state", normalized);
        modelRefreshStatus.setText(message == null || message.isBlank() ? t("console.modelSelector.refreshIdle") : message);
    }

    private void updateModelSelectionScopeStatus(boolean nextRunOnly) {
        if (modelSelectionScopeStatus == null) {
            return;
        }
        modelSelectionScopeStatus.getElement().setAttribute("data-selection-scope", nextRunOnly ? "next-run" : "future-runs");
        modelSelectionScopeStatus.setText(nextRunOnly
                ? t("console.modelSelector.appliesNextRun")
                : t("console.modelSelector.appliesFutureRuns"));
    }

    private void updateFallbackModeStatus() {
        if (fallbackModeStatus == null) {
            return;
        }
        fallbackModeStatus.setVisible(explicitLocalFallbackMode);
        if (explicitLocalFallbackMode) {
            fallbackModeStatus.getElement().setAttribute("data-fallback-mode", "local");
            fallbackModeStatus.setText(t("console.modelSelector.localFallback"));
        } else {
            fallbackModeStatus.getElement().removeAttribute("data-fallback-mode");
            fallbackModeStatus.setText("");
        }
    }

    private String localizedRefreshMessage(ProviderConfigController.ModelListResponse response) {
        String state = response.state() == null ? "error" : response.state();
        return switch (state) {
            case "success" -> t("console.modelSelector.refreshSuccess", response.modelCount());
            case "empty" -> t("console.modelSelector.refreshEmpty");
            case "not_configured" -> t("console.modelSelector.refreshNotConfigured");
            case "error" -> t("console.modelSelector.refreshError", safeRefreshDetail(response));
            default -> response.message() == null || response.message().isBlank()
                    ? t("console.modelSelector.refreshIdle")
                    : response.message();
        };
    }

    private static String safeRefreshDetail(ProviderConfigController.ModelListResponse response) {
        String detail = response.message() == null || response.message().isBlank() ? response.error() : response.message();
        if (detail == null || detail.isBlank()) {
            return "details redacted";
        }
        return detail.replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]")
                .replaceAll("(?i)bearer\\s+\\[REDACTED]", "[REDACTED credential]")
                .replaceAll("(?i)sk-[A-Za-z0-9._-]+", "[REDACTED]");
    }

    private static String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Map<String, Object> runMetadataSnapshot() {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("source", "vaadin-console");
        if (providerConfigStore == null) {
            return Map.copyOf(metadata);
        }
        ProviderConfig config = providerConfigStore.current();
        RunProviderMetadata snapshot = RunProviderMetadata.selectedSnapshot(config.providerId(), config.modelId(), config.isReady());
        putIfPresent(metadata, "requestedModelRef", snapshot.requestedModelRef());
        putIfPresent(metadata, "selectedModelRef", snapshot.selectedModelRef());
        putIfPresent(metadata, "resolvedProviderId", snapshot.resolvedProviderId());
        putIfPresent(metadata, "resolvedModelId", snapshot.resolvedModelId());
        putIfPresent(metadata, "providerId", snapshot.resolvedProviderId());
        putIfPresent(metadata, "modelId", snapshot.resolvedModelId());
        putIfPresent(metadata, "fallbackMode", snapshot.fallbackMode());
        putIfPresent(metadata, "readinessState", snapshot.readinessState());
        return Map.copyOf(metadata);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
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
                new CorrelationContext("00000000000000000000000000000001", "vaadin-console", null));
    }

    public RunSubmissionPlan planChatSubmission(String text) {
        String message = requireText(text, "text");
        if (!providerReadyForSend() && !explicitLocalFallbackMode) {
            showProviderBlockedSend();
            return null;
        }
        chatPanel.appendUserMessage(message);
        boolean needsSession = selectedSessionId == null;
        String sessionId = needsSession ? executionBridge.createSession().sessionId() : selectedSessionId;
        selectedSessionId = sessionId;
        CreateRunRequest request = new CreateRunRequest(
                selectedAgentId,
                "chat",
                Map.of("text", message),
                "console-default",
                runMetadataSnapshot());
        RunResponse run = executionBridge.createRun(sessionId, request);
        String runId = run.runId();
        activeRunId = runId;
        activeRunNextAfterSequence = 0;
        EventStreamClient.ConnectionSpec streamSpec = eventStreamClient.runEventStream(sessionId, runId, 0);
        ConversationEventReducer.apply(conversationEventReducer.begin(sessionId, runId, DEFAULT_ASSISTANT_STEP_ID), chatPanel, runEventRenderer);
        sessionListPanel.showSession(sessionId, sessionTitle(message), run.status(), latest(run.createdAt(), run.updatedAt()));
        sessionListPanel.selectSession(sessionId);
        recentSessionTitles.putIfAbsent(sessionId, sessionTitle(message));
        updateActiveSessionBanner(recentSessionTitles.get(sessionId));
        runContextPanel.showRunning(sessionId, runId);
        chatPanel.showComposerRunStatus("Run status: " + run.status(), isCancellable(run.status()));
        appendRunEvents(executionBridge.listEvents(sessionId, runId, 0));
        if (explicitLocalFallbackMode) {
            chatPanel.markLocalFallbackMode(t("chat.localFallback.label"));
        }
        subscribeToLiveRunEvents(runId);
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
        ConversationTranscriptResponse transcript = executionBridge.getTranscript(selectedSessionId, 100, null);
        chatPanel.replaceTranscript(transcript.messages());
        activeRunId = transcript.activeRunId();
        activeRunNextAfterSequence = parseNextAfterSequence(transcript.nextCursor());
        if (activeRunId != null && !activeRunId.isBlank()) {
            runContextPanel.showRunning(selectedSessionId, activeRunId);
            chatPanel.showComposerRunStatus("Run status: " + nullToDefault(transcript.activeRunStatus(), "running"), isCancellable(transcript.activeRunStatus()));
        }
        updateActiveSessionBanner(recentSessionTitles.get(selectedSessionId));
        showConsolePanel("chat");
        return new SessionSelectionPlan(selectedSessionId, httpClient.sessionHistoryPath(selectedSessionId));
    }

    public void loadRecentSessionsForProof() {
        PageResponse<SessionSummaryDto> recent = executionBridge.listRecentSessions(20, null);
        if (recent == null || recent.items() == null) {
            return;
        }
        recentSessionTitles.clear();
        for (SessionSummaryDto summary : recent.items()) {
            if (summary != null && summary.sessionId() != null && !summary.sessionId().isBlank()) {
                recentSessionTitles.put(summary.sessionId().trim(), summary.title());
            }
        }
        sessionListPanel.showRecentSessions(recent.items(), selectedSessionId, recent.hasMore());
        updateActiveSessionBanner(selectedSessionId == null ? null : recentSessionTitles.get(selectedSessionId));
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
        updateModelSelectionScopeStatus(false);
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
        // Mark the reducer and current assistant bubble stopped before the App call returns.
        // Provider abort is best-effort at lower layers; this local terminal key prevents
        // late model.delta events from mutating already-cancelled partial output.
        ConversationEventReducer.apply(
                conversationEventReducer.stopRun(selectedSessionId, activeRunId, DEFAULT_ASSISTANT_STEP_ID, reason),
                chatPanel,
                runEventRenderer);
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

    private static String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long parseNextAfterSequence(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(cursor.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
            activeRunNextAfterSequence = Math.max(activeRunNextAfterSequence, event.sequence());
            appended++;
            ConversationEventReducer.apply(conversationEventReducer.reduce(event), chatPanel, runEventRenderer);
            RunEventRenderer.RenderedEvent rendered = runEventRenderer.render(event);
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

    private boolean liveStreamingAvailable() {
        return liveRunEventSubscriber != null && liveRunEventSubscriber.available();
    }

    private boolean hasActiveRun() {
        return activeRunId != null && !activeRunId.isBlank();
    }

    private boolean providerReadyForSend() {
        return providerConfigStore == null || providerConfigStore.current().isReady();
    }

    private void showProviderBlockedSend() {
        ProviderConfig current = providerConfigStore == null ? ProviderConfig.defaults() : providerConfigStore.current();
        updateProviderStatus(false, current.providerId(), current.modelId());
        updateRefreshStatus("blocked", t("console.modelSelector.sendBlockedNotReady"));
        chatPanel.showComposerRunStatus(t("console.modelSelector.sendBlockedNotReady"), false);
    }

    private void subscribeToLiveRunEvents(String runId) {
        if (!liveStreamingAvailable()) {
            return;
        }
        liveRunEventSubscriber.subscribe(this, runId, event -> {
            if (event.sequence() <= activeRunNextAfterSequence) {
                return;
            }
            activeRunNextAfterSequence = Math.max(activeRunNextAfterSequence, event.sequence());
            ConversationEventReducer.apply(conversationEventReducer.reduce(event), chatPanel, runEventRenderer);
            RunEventRenderer.RenderedEvent rendered = runEventRenderer.render(event);
            if (event.type() != null && event.type().toLowerCase().contains("status") && event.payload() != null) {
                Object status = event.payload().get("status");
                if (status != null) {
                    applyRunStatus(String.valueOf(status), rendered.terminal());
                    return;
                }
            }
            if (rendered.terminal()) {
                applyRunStatus("terminal", true);
            }
        });
    }

    private Div createPanelSwitcher() {
        Div switcher = new Div();
        switcher.addClassName("pi-console-panel-switcher");
        switcher.getElement().setAttribute("data-role", "console-panel-switcher");
        switcher.add(panelButton(getTranslation("console.panel.chat"), "chat"));
        switcher.add(panelButton(getTranslation("console.panel.agents"), "agents"));
        switcher.add(panelButton(getTranslation("console.panel.sessions"), "sessions"));
        switcher.add(panelButton(getTranslation("console.panel.run"), "run-context"));
        switcher.getStyle().set("display", "flex");
        switcher.getStyle().set("gap", "0.5rem");
        switcher.getStyle().set("justify-content", "center");
        switcher.getStyle().set("flex-wrap", "wrap");
        switcher.getStyle().set("max-width", "820px");
        switcher.getStyle().set("margin", "0 auto 0.75rem");
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
        wrapper.getStyle().set("max-width", "820px");
        wrapper.getStyle().set("margin", "0 auto 0.75rem");
        return wrapper;
    }

    private void applyPanelState() {
        consolePanels.forEach((panel, wrapper) -> wrapper.getElement()
                .setAttribute("data-console-panel-active", Boolean.toString(panel.equals(activeConsolePanel))));
        consolePanels.forEach((panel, wrapper) -> wrapper.setVisible(panel.equals("chat") || panel.equals(activeConsolePanel)));
        panelControls.forEach((panel, button) -> button.getElement()
                .setAttribute("aria-pressed", Boolean.toString(panel.equals(activeConsolePanel))));
    }

    private String t(String key, Object... params) {
        String translated = getTranslation(key, params);
        if (!translated.startsWith("!{") || !translated.endsWith("}!")) {
            return translated;
        }
        try {
            String pattern = ResourceBundle.getBundle("messages").getString(key);
            return params == null || params.length == 0 ? pattern : MessageFormat.format(pattern, params);
        } catch (MissingResourceException ex) {
            return key;
        }
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

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
            SessionSummaryDto summary = new SessionSummaryDto(
                    "session-mobile-1", "Demo conversation", "ACTIVE", "hello", Instant.now(), Instant.now(), null, null, Map.of("source", "demo"));
            return new PageResponse<>(List.of(summary), limit, null, null, false);
        }

        @Override
        public ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
            ConversationMessageDto message = new ConversationMessageDto(
                    "message-demo-1", sessionId, null, null, ConversationMessageRole.USER, "hello", ConversationMessageStatus.COMPLETED,
                    Instant.now(), Instant.now(), null, null, Map.of("source", "demo"), true, false);
            return new ConversationTranscriptResponse(sessionId, List.of(message), null, null, null, false, Map.of("source", "demo"));
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
