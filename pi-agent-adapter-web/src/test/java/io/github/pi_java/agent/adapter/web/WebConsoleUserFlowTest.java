package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ChatEventStreamPanel;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunContextPanel;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.adapter.web.ui.console.SessionListPanel;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebConsoleUserFlowTest {

    @Test
    void consoleRouteIsChatFirstThreeColumnWorkbench() {
        ConsoleView view = new ConsoleView();

        assertThat(view.getElement().getAttribute("data-route")).isEqualTo("console");
        assertThat(view.getElement().getAttribute("data-layout")).isEqualTo("three-column-workbench");
        assertThat(view.chatPanel().getElement().getAttribute("data-primary")).isEqualTo("chat-first");
        assertThat(view.chatPanel().inputPlaceholder()).containsIgnoringCase("message");

        assertThat(view.sessionListPanel()).isInstanceOf(SessionListPanel.class);
        assertThat(view.chatPanel()).isInstanceOf(ChatEventStreamPanel.class);
        assertThat(view.runContextPanel()).isInstanceOf(RunContextPanel.class);
        assertThat(view.columnOrder()).containsExactly("sessions", "chat-event-stream", "run-context");
        assertThat(view.selectedAgentId()).isEqualTo("cloud-general-agent");
        assertThat(view.agentCatalogPlan().path()).isEqualTo("/api/agents");
    }

    @Test
    void emptyAndRecentSessionStatesRenderInLeftColumn() {
        SessionListPanel panel = new SessionListPanel();

        assertThat(panel.emptyStateText()).containsIgnoringCase("no recent sessions");

        panel.showSession("session-1", "General Agent", Instant.parse("2026-06-15T05:00:00Z"));

        assertThat(panel.sessionCount()).isEqualTo(1);
        assertThat(panel.recentSessionIds()).containsExactly("session-1");
        assertThat(panel.getElement().getText()).contains("session-1").contains("General Agent");
    }

    @Test
    void sendingChatCreatesOrReusesSessionCreatesChatRunAndSubscribesToSse() {
        ConsoleView view = new ConsoleView();

        ConsoleView.RunSubmissionPlan first = view.planChatSubmission("Summarize the current workspace");

        assertThat(first.createSessionPath()).isEqualTo("/api/sessions");
        assertThat(first.sessionId()).isEqualTo("pending-session");
        assertThat(first.createRunPath()).isEqualTo("/api/sessions/pending-session/runs");
        assertThat(first.request().agentId()).isEqualTo("cloud-general-agent");
        assertThat(first.request().inputType()).isEqualTo("chat");
        assertThat(first.request().input()).containsEntry("text", "Summarize the current workspace");
        assertThat(first.streamSpec().url()).isEqualTo("/api/sessions/pending-session/runs/pending-run/stream?afterSequence=0");
        assertThat(view.chatPanel().messageCount()).isEqualTo(1);

        view.selectSession("session-42");
        ConsoleView.RunSubmissionPlan continuation = view.planChatSubmission("Continue from history");

        assertThat(continuation.createSessionPath()).isNull();
        assertThat(continuation.sessionId()).isEqualTo("session-42");
        assertThat(continuation.createRunPath()).isEqualTo("/api/sessions/session-42/runs");
    }

    @Test
    void selectingPriorSessionLoadsHistoryAndContinuesSameSession() {
        ConsoleView view = new ConsoleView();

        ConsoleView.SessionSelectionPlan selection = view.selectSession("session-history");
        ConsoleView.RunSubmissionPlan continuation = view.planChatSubmission("next message");

        assertThat(selection.historyPath()).isEqualTo("/api/sessions/session-history/history");
        assertThat(view.sessionListPanel().selectedSessionId()).isEqualTo("session-history");
        assertThat(continuation.sessionId()).isEqualTo("session-history");
        assertThat(continuation.createRunPath()).isEqualTo("/api/sessions/session-history/runs");
    }

    @Test
    void cancelButtonCallsPublicCancelApiAndShowsStatusFeedback() {
        ConsoleView view = new ConsoleView();
        view.markRunRunning("session-1", "run-1");

        ConsoleView.CancelPlan plan = view.planCancelRunningRun("user requested stop");

        assertThat(plan.cancelPath()).isEqualTo("/api/sessions/session-1/runs/run-1/cancel");
        assertThat(plan.request().reason()).isEqualTo("user requested stop");
        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelling");
        assertThat(view.runContextPanel().cancelProminent()).isTrue();

        view.applyRunStatus("cancelled", true);

        assertThat(view.runContextPanel().statusText()).containsIgnoringCase("cancelled");
        assertThat(view.runContextPanel().cancelProminent()).isFalse();
    }

    @Test
    void rendererIntegratesModelRunPolicyAndTerminalEventsInOneNarrative() {
        RunEventRenderer renderer = new RunEventRenderer();

        assertThat(renderer.render(event("model.delta", Map.of("text", "hello"))).text()).contains("hello");
        assertThat(renderer.render(event("run.status", Map.of("status", "RUNNING"))).text()).contains("RUNNING");
        assertThat(renderer.render(event("tool.lifecycle", Map.of("toolName", "search", "status", "REQUIRE_APPROVAL"))).text())
                .contains("search")
                .contains("REQUIRE_APPROVAL");
        assertThat(renderer.render(event("run.completed", Map.of("status", "COMPLETED"))).terminal()).isTrue();

        EventStreamClient.ConnectionSpec spec = new EventStreamClient().runEventStream("session-1", "run-1", 5);
        assertThat(spec.url()).isEqualTo("/api/sessions/session-1/runs/run-1/stream?afterSequence=5");
    }

    private static RunEventDto event(String type, Map<String, Object> payload) {
        return new RunEventDto(
                "event-1",
                "tenant",
                "user",
                "session-1",
                "run-1",
                null,
                "workspace",
                1,
                Instant.parse("2026-06-15T05:00:00Z"),
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
