package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger;
import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.util.Map;

/** Default Console execution bridge that keeps Vaadin concerns in adapter-web and delegates to App use cases. */
public final class AppConsoleRunExecutionBridge implements ConsoleRunExecutionBridge {

    private final SessionCommandService sessionCommandService;
    private final RunCommandService runCommandService;
    private final RunQueryService runQueryService;
    private final RunActivationTrigger runActivationTrigger;
    private final RunDispatcher runDispatcher;

    public AppConsoleRunExecutionBridge(
            SessionCommandService sessionCommandService,
            RunCommandService runCommandService,
            RunQueryService runQueryService,
            RunActivationTrigger runActivationTrigger,
            RunDispatcher runDispatcher) {
        this.sessionCommandService = sessionCommandService;
        this.runCommandService = runCommandService;
        this.runQueryService = runQueryService;
        this.runActivationTrigger = runActivationTrigger;
        this.runDispatcher = runDispatcher;
    }

    @Override
    public SessionResponse createSession() {
        return sessionCommandService.createSession(
                ConsoleView.consoleRequestContext(),
                new CreateSessionRequest("console-default", Map.of("source", "vaadin-console")));
    }

    @Override
    public RunResponse createRun(String sessionId, CreateRunRequest request) {
        RunResponse response = runCommandService.createRun(ConsoleView.consoleRequestContext(), sessionId, request);
        runActivationTrigger.triggerAsync();
        runDispatcher.dispatch("vaadin-console-worker");
        return response;
    }

    @Override
    public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
        return runQueryService.listEvents(ConsoleView.consoleRequestContext(), sessionId, runId, afterSequence, 500);
    }

    @Override
    public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
        return runCommandService.cancelRun(ConsoleView.consoleRequestContext(), sessionId, runId, request);
    }
}
