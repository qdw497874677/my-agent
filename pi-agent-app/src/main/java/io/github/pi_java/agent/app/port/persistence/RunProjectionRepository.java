package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface RunProjectionRepository {

    void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request);

    Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId);

    RunStatusResponse getStatus(RequestContext context, String sessionId, String runId);

    boolean markRunning(String runId, Instant startedAt);

    boolean requestCancellation(String runId, String reason, Instant requestedAt);

    boolean markTerminalIfNotTerminal(
            String runId,
            String status,
            Map<String, Object> terminalResult,
            Map<String, Object> failure,
            Instant finishedAt);

    default void updateLastEventSequence(String runId, long sequence, Instant updatedAt) {
    }

    RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId);

    PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit);

    PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit);

    PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit);

    RunResultResponse getRunResult(RequestContext context, String sessionId, String runId);
}
