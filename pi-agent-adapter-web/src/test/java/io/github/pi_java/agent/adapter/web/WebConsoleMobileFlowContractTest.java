package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import io.github.pi_java.agent.adapter.web.ui.console.AgentCatalogPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.SessionListPanel;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebConsoleMobileFlowContractTest {

    @Test
    void consoleOnlyMountsProviderConfigurationAndChatSurface() {
        ConsoleView view = new ConsoleView();

        assertThat(descendants(view.getElement())
                .filter(element -> "show-console-panel".equals(element.getAttribute("data-action"))))
                .isEmpty();
        assertPanelState(view, "chat", "true");
        assertThat(descendants(view.getElement())
                .filter(element -> "agents".equals(element.getAttribute("data-console-panel"))
                        || "sessions".equals(element.getAttribute("data-console-panel"))
                        || "run-context".equals(element.getAttribute("data-console-panel"))))
                .isEmpty();
    }

    @Test
    void chatPanelIsActiveByDefaultAndDesktopColumnContractRemains() {
        ConsoleView view = new ConsoleView();

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertPanelState(view, "chat", "true");
        assertThat(view.getElement().getAttribute("data-layout")).isEqualTo("chat-home");
        assertThat(view.columnOrder()).containsExactly("provider-config", "chat-event-stream");
    }

    @Test
    void chatOnlySurfacePreservesExistingChatPanelAndMessages() {
        ConsoleView view = new ConsoleView();
        ChatEventStreamPanel originalChatPanel = view.chatPanel();

        view.planChatSubmission("Keep this message while browsing sessions");
        view.showConsolePanel("chat");

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertThat(view.chatPanel()).isSameAs(originalChatPanel);
        assertThat(view.chatPanel().messages()).contains("Keep this message while browsing sessions");
        assertPanelState(view, "chat", "true");
    }

    @Test
    void chatPanelSplitsVerticalEventFeedFromStickyComposer() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        Div feed = onlyChildWithAttribute(panel, "data-role", "event-feed");
        Div composer = onlyChildWithAttribute(panel, "data-role", "chat-composer");

        assertThat(feed.getClassNames()).contains("pi-console-event-feed");
        assertThat(composer.getClassNames()).contains("pi-console-composer");
        assertThat(onlyDescendantWithAttribute(composer, "data-role", "composer-run-status").getElement().getTextRecursively())
                .isNotBlank();
        assertThat(onlyDescendantWithAttribute(composer, "data-role", "chat-input")).isNotNull();
        assertThat(onlyDescendantWithAttribute(composer, "data-action", "send-chat")).isNotNull();
        assertThat(composer.getElement().hasAttribute("data-action")).isFalse();
        Component cancel = onlyDescendantWithAttribute(composer, "data-action", "cancel-run");
        assertThat(cancel).isInstanceOf(Button.class);
        assertThat(cancel.getElement().getAttribute("data-action-compatibility")).isEqualTo("cancel-run-primary");
        assertThat(cancel.getElement().getAttribute("data-action-scope"))
                .isEqualTo("primary-composer");
        assertThat(panel.composerCancelVisible()).isFalse();
    }

    @Test
    void chatComposerUsesBoundedMultilineTextAreaRows() {
        ChatEventStreamPanel panel = new ChatEventStreamPanel();

        assertThat(panel.inputMinRows()).isEqualTo(2);
        assertThat(panel.inputMaxRows()).isEqualTo(6);
    }

    @Test
    void chatSubmissionSynchronizesRunStateAcrossComposerAndRunContext() {
        ConsoleView view = new ConsoleView();

        view.planChatSubmission("Run this mobile prompt");

        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("running");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("running");
        assertThat(view.runContextPanel().cancelProminent()).isTrue();
        assertThat(view.chatPanel().composerCancelVisible()).isTrue();
    }

    @Test
    void cancellingAndTerminalStatusesUpdateBothVisibleRunSurfaces() {
        ConsoleView view = new ConsoleView();
        view.markRunRunning("session-1", "run-1");

        view.planCancelRunningRun("mobile cancel");

        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelled");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("cancelled");
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
        assertThat(view.chatPanel().composerCancelVisible()).isFalse();

        view.applyRunStatus("completed", true);

        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("completed");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("completed");
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
        assertThat(view.chatPanel().composerCancelVisible()).isFalse();
    }

    @Test
    void backupAndPrimaryCancelControlsExposeStableHooks() {
        ConsoleView view = new ConsoleView();

        Component primaryCancel = onlyDescendantWithAttribute(view.chatPanel(), "data-action", "cancel-run");
        assertThat(primaryCancel).isInstanceOf(Button.class);
        assertThat(primaryCancel.getElement().getAttribute("data-action-scope"))
                .isEqualTo("primary-composer");
        assertThat(primaryCancel.getElement().getAttribute("data-action-compatibility")).isEqualTo("cancel-run-primary");
        assertThat(onlyDescendantWithAttribute(view.runContextPanel(), "data-action", "cancel-run").getClassNames())
                .contains("pi-console-cancel-backup");
        assertThat(onlyDescendantWithAttribute(view.runContextPanel(), "data-role", "run-status")).isNotNull();
    }

    @Test
    void agentCatalogCardsExposeGeneralAgentPrimaryActionContract() {
        AgentCatalogPanel panel = new AgentCatalogPanel();

        panel.showCatalog(new AgentCatalogResponse(List.of(generalAgent("cloud-general-agent", "start-chat"))));

        assertThat(panel.getClassNames()).contains("pi-agent-catalog-panel");
        assertThat(panel.cardsElement().getElement().getAttribute("data-role")).isEqualTo("agent-catalog-cards");
        assertThat(panel.renderedCards().getFirst().getClassNames()).contains("pi-agent-card");
        assertThat(panel.renderedCards().getFirst().getElement().getAttribute("data-agent-id"))
                .isEqualTo("cloud-general-agent");
        assertThat(panel.renderedCards().getFirst().getElement().getAttribute("data-general-agent")).isEqualTo("true");
        assertThat(panel.renderedCards().getFirst().primaryActionMarker()).isEqualTo("general-agent-start");
        assertThat(panel.renderedCards().getFirst().firstEntryAction()).isEqualTo("start-chat");
    }

    @Test
    void sessionHistoryRendersCardMetadataAndActiveIdentity() {
        SessionListPanel panel = new SessionListPanel();

        panel.showSession("session-a", "Architecture review", Instant.parse("2026-06-23T05:00:00Z"));
        panel.selectSession("session-a");

        Div card = panel.sessionCards().getFirst();
        assertThat(panel.listElement().getElement().getAttribute("data-role")).isEqualTo("session-list");
        assertThat(card.getClassNames()).contains("pi-session-card");
        assertThat(card.getElement().getAttribute("data-role")).isEqualTo("session-card");
        assertThat(card.getElement().getAttribute("data-session-id")).isEqualTo("session-a");
        assertThat(card.getElement().getAttribute("data-session-active")).isEqualTo("true");
        assertThat(fieldText(card, "session-title")).contains("Architecture review");
        assertThat(fieldText(card, "session-status")).contains("ready");
        assertThat(fieldText(card, "session-updated-at")).contains("2026-06-23T05:00:00Z");
    }

    @Test
    void selectingPriorSessionMarksActiveCardAndReturnsToChatPanel() {
        ConsoleView view = new ConsoleView();

        view.selectSession("session-history");

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-history");
        assertThat(view.sessionListPanel().sessionCards().getFirst().getElement().getAttribute("data-session-active"))
                .isEqualTo("true");
    }

    @Test
    void mcon01CatalogPanelIsNotMountedOnChatOnlyConsoleSurface() {
        ConsoleView view = new ConsoleView();

        assertThat(descendantComponents(view)).doesNotContain(view.agentCatalogPanel());
        assertThat(descendantComponents(view)).doesNotContain(view.sessionListPanel());
        assertThat(descendantComponents(view)).doesNotContain(view.runContextPanel());
    }

    @Test
    void mcon01GeneralAgentPrimaryActionUsesRealClickHandlerAndReturnsToChat() {
        ConsoleView view = new ConsoleView();
        view.agentCatalogPanel().showCatalog(new AgentCatalogResponse(List.of(generalAgent("cloud-general-agent", "start-chat"))));

        Button generalAgentCta = (Button) onlyDescendantWithAttribute(view.agentCatalogPanel(), "data-primary-action", "general-agent-start");
        generalAgentCta.click();

        assertThat(view.selectedAgentId()).isEqualTo("cloud-general-agent");
        assertThat(view.activeConsolePanel()).isEqualTo("chat");
    }

    @Test
    void mcon04SessionCardActivatesByClickEnterAndSpaceAndReturnsToChat() {
        ConsoleView clickView = new ConsoleView();
        clickView.sessionListPanel().showSession("session-click", "Click session", Instant.parse("2026-06-23T05:00:00Z"));

        clickView.sessionListPanel().activateSessionCardForTest("session-click", "click");

        assertThat(clickView.sessionListPanel().selectedSessionId()).isEqualTo("session-click");
        assertThat(clickView.activeConsolePanel()).isEqualTo("chat");
        assertThat(clickView.sessionListPanel().sessionCards().getFirst().getElement().getAttribute("data-session-active"))
                .isEqualTo("true");

        ConsoleView enterView = new ConsoleView();
        enterView.sessionListPanel().showSession("session-enter", "Enter session", Instant.parse("2026-06-23T05:00:00Z"));
        enterView.sessionListPanel().activateSessionCardForTest("session-enter", "Enter");
        assertThat(enterView.sessionListPanel().selectedSessionId()).isEqualTo("session-enter");
        assertThat(enterView.activeConsolePanel()).isEqualTo("chat");

        ConsoleView spaceView = new ConsoleView();
        spaceView.sessionListPanel().showSession("session-space", "Space session", Instant.parse("2026-06-23T05:00:00Z"));
        spaceView.sessionListPanel().activateSessionCardForTest("session-space", " ");
        assertThat(spaceView.sessionListPanel().selectedSessionId()).isEqualTo("session-space");
        assertThat(spaceView.activeConsolePanel()).isEqualTo("chat");
    }

    @Test
    void mcon02Mcon05SendAndCancelControlsUseListenerBackedVaadinClicks() {
        ConsoleView view = new ConsoleView();
        TextArea input = (TextArea) onlyDescendantWithAttribute(view.chatPanel(), "data-role", "chat-input");
        Button send = (Button) onlyDescendantWithAttribute(view.chatPanel(), "data-action", "send-chat");

        input.setValue("Run from actual Send click");
        send.click();

        assertThat(view.chatPanel().messages()).contains("Run from actual Send click");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("running");

        Button primaryCancel = (Button) onlyDescendantWithAttribute(view.chatPanel(), "data-action", "cancel-run");
        primaryCancel.click();
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("cancelled");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelled");

        view.markRunRunning("session-backup", "run-backup");
        Button backupCancel = (Button) onlyDescendantWithAttribute(view.runContextPanel(), "data-action", "cancel-run");
        backupCancel.click();
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("cancelled");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelled");
    }

    @Test
    void mcon01NormalConsoleConstructionRendersGeneralAgentFromExistingCatalogReadModel() {
        ConsoleView view = new ConsoleView(new DefaultAgentCatalogQueryService());

        assertThat(view.agentCatalogPanel().renderedAgentIds()).contains("cloud-general-agent");
        assertThat(view.agentCatalogPanel().cardCount()).isGreaterThan(0);

        assertThat(descendantComponents(view)).doesNotContain(view.agentCatalogPanel());
    }

    @Test
    void mcon04HistoricalSessionsRemainExplicitlyEmptyUntilExistingHistoryModelProvidesRows() {
        ConsoleView view = new ConsoleView(new DefaultAgentCatalogQueryService());

        assertThat(view.sessionListPanel().sessionCount()).isZero();
        assertThat(view.sessionListPanel().emptyStateText()).isNotBlank();

        view.sessionListPanel().showSession("session-history", "History row from existing read model", Instant.parse("2026-06-23T05:00:00Z"));
        view.sessionListPanel().activateSessionCardForTest("session-history", "Enter");

        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-history");
        assertThat(view.activeConsolePanel()).isEqualTo("chat");
    }

    @Test
    void mcon02UserTriggeredSubmitUsesDtoBackedSessionAndRunIds() {
        ConsoleView view = new ConsoleView();

        ConsoleView.RunSubmissionPlan plan = view.planChatSubmission("First line\nsecond line");

        assertThat(plan.sessionId()).isEqualTo("session-mobile-1");
        assertThat(plan.streamSpec().url()).contains("/runs/run-mobile-1/stream");
        assertThat(plan.request().input()).containsEntry("text", "First line\nsecond line");
        assertThat(view.chatPanel().messages()).contains("First line\nsecond line");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("running");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("running");
        assertThat(view.chatPanel().composerCancelVisible()).isTrue();
    }

    @Test
    void mcon04SuccessfulSendAddsVisibleActiveSessionCardWithPromptTitle() {
        ConsoleView view = new ConsoleView();

        view.planChatSubmission("Summarize mobile session history after send");

        SessionListPanel sessions = view.sessionListPanel();
        assertThat(sessions.sessionCount()).isEqualTo(1);
        assertThat(sessions.selectedSessionId()).isEqualTo("session-mobile-1");
        Div card = sessions.sessionCards().getFirst();
        assertThat(card.getElement().getAttribute("data-role")).isEqualTo("session-card");
        assertThat(card.getElement().getAttribute("data-session-id")).isEqualTo("session-mobile-1");
        assertThat(card.getElement().getAttribute("data-session-active")).isEqualTo("true");
        assertThat(fieldText(card, "session-title")).contains("Summarize mobile session history after send");
        assertThat(fieldText(card, "session-status")).containsIgnoringCase("queued");
        assertThat(fieldText(card, "session-updated-at")).contains("2026");
    }

    @Test
    void mcon04ActivatingCreatedSessionReturnsToChatAndNextSendContinuesSameSession() {
        ConsoleView view = new ConsoleView();

        view.planChatSubmission("Create selectable session card");
        view.sessionListPanel().activateSessionCardForTest("session-mobile-1", "click");
        ConsoleView.RunSubmissionPlan continued = view.planChatSubmission("Continue same selected session");

        assertThat(view.activeConsolePanel()).isEqualTo("chat");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-mobile-1");
        assertThat(continued.createSessionPath()).isNull();
        assertThat(continued.sessionId()).isEqualTo("session-mobile-1");
        assertThat(view.sessionListPanel().sessionCount()).isEqualTo(1);
        assertThat(view.sessionListPanel().sessionCards().getFirst().getElement().getAttribute("data-session-active"))
                .isEqualTo("true");
    }

    @Test
    void mcon03SubmitRendersRunHistoryEventsThroughFeedRenderer() {
        ConsoleView view = new ConsoleView();

        view.planChatSubmission("Render replayed events");

        assertThat(view.chatPanel().messageCount()).isGreaterThan(1);
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("running");
        assertThat(view.chatPanel().messages()).anySatisfy(message -> assertThat(message).contains("model reply"));
    }

    @Test
    void mcon03AppendsLaterRunEventsAfterCreateRunWithoutAnotherSend() {
        ReplayBridge bridge = new ReplayBridge(
                new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(), 0, 0, false),
                new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(
                        event("run.status", Map.of("status", "RUNNING"), 1),
                        event("model.delta", Map.of("text", "later model reply"), 2)
                ), 0, 2, false));
        ConsoleView view = new ConsoleView(
                new io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient(),
                new io.github.pi_java.agent.adapter.web.ui.EventStreamClient(),
                new DefaultAgentCatalogQueryService(),
                bridge);

        view.planChatSubmission("Start run before later events exist");
        int afterCreateRunCount = view.chatPanel().messageCount();

        int appended = view.refreshActiveRunEvents();

        assertThat(appended).isEqualTo(2);
        assertThat(view.chatPanel().messageCount()).isEqualTo(afterCreateRunCount + 1);
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("running");
        assertThat(view.chatPanel().messages()).anySatisfy(message -> assertThat(message).contains("later model reply"));
        assertThat(bridge.afterSequences()).containsExactly(0L, 0L);
    }

    @Test
    void mcon03ReplayRefreshDoesNotAppendDuplicateSequences() {
        EventHistoryResponse laterEvents = new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(
                event("model.delta", Map.of("text", "deduplicated reply"), 1)
        ), 0, 1, false);
        ReplayBridge bridge = new ReplayBridge(
                new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(), 0, 0, false),
                laterEvents,
                laterEvents);
        ConsoleView view = new ConsoleView(
                new io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient(),
                new io.github.pi_java.agent.adapter.web.ui.EventStreamClient(),
                new DefaultAgentCatalogQueryService(),
                bridge);

        view.planChatSubmission("Start run before duplicate replay");
        assertThat(view.refreshActiveRunEvents()).isEqualTo(1);
        int afterFirstRefresh = view.chatPanel().messageCount();

        assertThat(view.refreshActiveRunEvents()).isZero();
        assertThat(view.chatPanel().messageCount()).isEqualTo(afterFirstRefresh);
    }

    @Test
    void mcon03TerminalLaterEventsUpdateBothRunStatusSurfaces() {
        ReplayBridge bridge = new ReplayBridge(
                new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(), 0, 0, false),
                new EventHistoryResponse("session-mobile-1", "run-mobile-1", List.of(
                        event("run.status", Map.of("status", "COMPLETED"), 3)
                ), 0, 3, false));
        ConsoleView view = new ConsoleView(
                new io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient(),
                new io.github.pi_java.agent.adapter.web.ui.EventStreamClient(),
                new DefaultAgentCatalogQueryService(),
                bridge);

        view.planChatSubmission("Start run before terminal event");

        assertThat(view.refreshActiveRunEvents()).isEqualTo(1);
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("completed");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("completed");
        assertThat(view.chatPanel().composerCancelVisible()).isFalse();
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
        assertThat(view.refreshActiveRunEvents()).isZero();
        assertThat(bridge.afterSequences()).containsExactly(0L, 0L);
    }

    @Test
    void mcon05CancelAppliesDtoBackedCancellationResultToBothSurfaces() {
        ConsoleView view = new ConsoleView();
        view.planChatSubmission("Cancel this run");

        ConsoleView.CancelPlan cancel = view.planCancelRunningRun("mobile cancel");

        assertThat(cancel.cancelPath()).isEqualTo("/api/sessions/session-mobile-1/runs/run-mobile-1/cancel");
        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("cancelled");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelled");
        assertThat(view.chatPanel().composerCancelVisible()).isFalse();
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
    }

    @Test
    void mcon05DoubleClickOrNoActiveRunCancelShowsSafeTerminalFeedback() {
        ConsoleView view = new ConsoleView();
        view.planChatSubmission("Double cancel this run");
        Button primaryCancel = (Button) onlyDescendantWithAttribute(view.chatPanel(), "data-action", "cancel-run");

        primaryCancel.click();
        primaryCancel.click();

        assertThat(view.chatPanel().composerStatusText()).containsIgnoringCase("no active run");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("no active run");
        assertThat(view.chatPanel().composerCancelVisible()).isFalse();
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
    }

    @SuppressWarnings("unused")
    private static final class DeterministicConsoleExecutionFixtures {
        private final SessionResponse session = new SessionResponse(
                "tenant", "user", "session-mobile-1", "workspace", null, "ACTIVE", now(), now(), Map.of());
        private final RunResponse run = new RunResponse(
                "tenant", "user", "session-mobile-1", "run-mobile-1", "workspace", "QUEUED", "trace", "correlation", now(), now());
        private final EventHistoryResponse events = new EventHistoryResponse(
                "session-mobile-1", "run-mobile-1", List.of(
                event("run.status", Map.of("status", "RUNNING"), 1),
                event("model.delta", Map.of("text", "model reply"), 2)
        ), 0, 2, false);
        private final RunStatusResponse cancelled = new RunStatusResponse(
                "session-mobile-1", "run-mobile-1", "cancelled", true, now(), "trace", "correlation");
    }

    private static void assertPanelState(ConsoleView view, String panel, String active) {
        Div wrapper = onlyChildWithAttribute(view, "data-console-panel", panel);
        assertThat(wrapper.getElement().getAttribute("data-console-panel-active")).isEqualTo(active);
    }

    private static Div onlyChildWithAttribute(ConsoleView view, String attribute, String value) {
        List<Div> matches = view.getChildren()
                .filter(Div.class::isInstance)
                .map(Div.class::cast)
                .filter(child -> value.equals(child.getElement().getAttribute(attribute)))
                .toList();
        assertThat(matches).extracting(Component::getElement).hasSize(1);
        return matches.getFirst();
    }

    private static Div onlyChildWithAttribute(ChatEventStreamPanel panel, String attribute, String value) {
        List<Div> matches = panel.getChildren()
                .filter(Div.class::isInstance)
                .map(Div.class::cast)
                .filter(child -> value.equals(child.getElement().getAttribute(attribute)))
                .toList();
        assertThat(matches).extracting(Component::getElement).hasSize(1);
        return matches.getFirst();
    }

    private static Component onlyDescendantWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = descendants(root.getElement())
                .filter(element -> value.equals(element.getAttribute(attribute)))
                .map(element -> element.getComponent().orElseThrow())
                .toList();
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }

    private static java.util.stream.Stream<Element> descendants(Element root) {
        return root.getChildren().flatMap(child -> java.util.stream.Stream.concat(java.util.stream.Stream.of(child), descendants(child)));
    }

    private static List<Component> descendantComponents(Component root) {
        return descendants(root.getElement())
                .flatMap(element -> element.getComponent().stream())
                .toList();
    }

    private static String fieldText(Div card, String field) {
        return card.getChildren()
                .filter(component -> field.equals(component.getElement().getAttribute("data-field")))
                .findFirst()
                .map(Component::getElement)
                .map(element -> element.getTextRecursively())
                .orElse("");
    }

    private static AgentCatalogItemDto generalAgent(String id, String actionId) {
        return new AgentCatalogItemDto(
                id,
                "Cloud General Agent",
                "General purpose agent for mobile console testing",
                Set.of("chat"),
                Set.of("streaming"),
                new AgentCatalogItemDto.ModelRefDto("openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini"),
                Set.of("builtin.info"),
                Set.of("tool:read"),
                Set.of("LOW"),
                Set.of("READ_ONLY"),
                List.of(new AgentCatalogItemDto.EntryActionDto(actionId, "Start chat", "CREATE_RUN", "CHAT", Map.of())),
                Duration.ofSeconds(30),
                Map.of("source", "test"));
    }

    private static final class ReplayBridge implements io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge {
        private final List<EventHistoryResponse> histories;
        private final List<Long> afterSequences = new java.util.ArrayList<>();
        private int historyIndex;

        private ReplayBridge(EventHistoryResponse... histories) {
            this.histories = List.of(histories);
        }

        @Override
        public SessionResponse createSession() {
            return new SessionResponse(
                    "tenant", "user", "session-mobile-1", "workspace", null, "ACTIVE", now(), now(), Map.of("source", "test"));
        }

        @Override
        public RunResponse createRun(String sessionId, io.github.pi_java.agent.client.run.CreateRunRequest request) {
            return new RunResponse("tenant", "user", sessionId, "run-mobile-1", "workspace", "QUEUED", "trace", "correlation", now(), now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            afterSequences.add(afterSequence);
            EventHistoryResponse history = histories.get(Math.min(historyIndex, histories.size() - 1));
            historyIndex++;
            return history;
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, io.github.pi_java.agent.client.run.CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "cancelled", true, now(), "trace", "correlation");
        }

        private List<Long> afterSequences() {
            return List.copyOf(afterSequences);
        }
    }

    private static RunEventDto event(String type, Map<String, Object> payload, long sequence) {
        return new RunEventDto(
                "event-" + sequence,
                "tenant",
                "user",
                "session-mobile-1",
                "run-mobile-1",
                null,
                "workspace",
                sequence,
                now(),
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

    private static Instant now() {
        return Instant.parse("2026-06-23T06:00:00Z");
    }
}
