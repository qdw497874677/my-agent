package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
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

    /**
     * Lists the runs that belong to {@code sessionId} for the caller defined by
     * {@link RequestContext}, ordered by run creation for stable transcript
     * assembly (decisions D-09, D-16).
     *
     * <p>Each item is a {@link ConversationRunView} carrying the run identity,
     * creation timestamp, input map (source of the user message text), and
     * status needed by the App transcript assembler. Infrastructure
     * implementations must enforce tenant/user/session ownership filters at the
     * query/SQL layer (decision D-15). {@code cursor} is an opaque,
     * implementation-defined pagination cursor; {@code null} starts from the
     * oldest run.
     *
     * <p>This method is a default that throws until Phase 16 plan 03 provides
     * the ownership-aware JDBC implementation; it is declared as a default so
     * existing test fakes and infrastructure stubs continue to compile.
     *
     * @return a page of conversation run views, never {@code null}.
     */
    default PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor) {
        throw new UnsupportedOperationException(
                "listRunsBySession not yet implemented; Phase 16 plan 03 provides the ownership-aware implementation");
    }
}
