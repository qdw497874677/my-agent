package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;

/** Adapter-web seam used by the Vaadin Console to execute public run/session semantics. */
public interface ConsoleRunExecutionBridge {

    SessionResponse createSession();

    RunResponse createRun(String sessionId, CreateRunRequest request);

    EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence);

    RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);
}
