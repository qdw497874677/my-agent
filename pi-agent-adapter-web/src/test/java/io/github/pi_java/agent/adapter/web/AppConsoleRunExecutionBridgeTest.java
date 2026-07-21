package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger;
import io.github.pi_java.agent.adapter.web.ui.console.AppConsoleRunExecutionBridge;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConsoleRunExecutionBridgeTest {

    @Test
    void createRunTriggersWorkerAsynchronouslyWithoutBlockingOnDispatcher() {
        RecordingRunCommandService runCommandService = new RecordingRunCommandService();
        RecordingActivationTrigger activationTrigger = new RecordingActivationTrigger();
        RecordingRunDispatcher dispatcher = new RecordingRunDispatcher();
        AppConsoleRunExecutionBridge bridge = new AppConsoleRunExecutionBridge(
                new UnsupportedSessionCommandService(),
                runCommandService,
                new UnsupportedRunQueryService(),
                new UnsupportedConversationQueryService(),
                activationTrigger,
                dispatcher);
        CreateRunRequest request = new CreateRunRequest("agent", "chat", Map.of("text", "hello"), "workspace", Map.of());

        RunResponse response = bridge.createRun("session-1", request);

        assertThat(response.runId()).isEqualTo("run-1");
        assertThat(runCommandService.sessionId).isEqualTo("session-1");
        assertThat(activationTrigger.calls).isEqualTo(1);
        assertThat(dispatcher.dispatchCalls).isZero();
        assertThat(dispatcher.dispatchRunCalls).isZero();
    }

    private static final class RecordingRunCommandService implements RunCommandService {
        private String sessionId;

        @Override
        public RunResponse createRun(RequestContext context, String sessionId, CreateRunRequest request) {
            this.sessionId = sessionId;
            return new RunResponse("tenant", "user", sessionId, "run-1", "workspace", "QUEUED", "trace", "correlation", Instant.now(), Instant.now());
        }

        @Override
        public RunStatusResponse cancelRun(RequestContext context, String sessionId, String runId, CancelRunRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingActivationTrigger implements RunActivationTrigger {
        private int calls;

        @Override
        public void triggerAsync() {
            calls++;
        }
    }

    private static final class RecordingRunDispatcher implements RunDispatcher {
        private int dispatchCalls;
        private int dispatchRunCalls;

        @Override
        public void dispatch(String workerId) {
            dispatchCalls++;
        }

        @Override
        public void dispatchRun(String workerId, String runId) {
            dispatchRunCalls++;
        }
    }

    private static final class UnsupportedSessionCommandService implements SessionCommandService {
        @Override
        public SessionResponse createSession(RequestContext context, CreateSessionRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class UnsupportedRunQueryService implements RunQueryService {
        @Override
        public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RunStatusResponse getRunStatus(RequestContext context, String sessionId, String runId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventHistoryResponse listEvents(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class UnsupportedConversationQueryService implements ConversationQueryService {
        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
            throw new UnsupportedOperationException();
        }
    }
}
