package io.github.pi_java.agent.adapter.web.ui.console;

import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.util.List;
import java.util.Map;

/** Adapter-web seam used by the Vaadin Console to execute public run/session semantics. */
public interface ConsoleRunExecutionBridge {

    SessionResponse createSession();

    RunResponse createRun(String sessionId, CreateRunRequest request);

    EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence);

    RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);

    default PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor) {
        return new PageResponse<>(List.of(), limit, null, null, false);
    }

    default ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor) {
        return new ConversationTranscriptResponse(sessionId, List.of(), null, null, null, false, Map.of());
    }
}
