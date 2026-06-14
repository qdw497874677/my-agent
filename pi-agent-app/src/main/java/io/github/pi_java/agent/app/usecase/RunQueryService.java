package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;

import java.util.Map;

public interface RunQueryService {

    RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId);

    RunStatusResponse getRunStatus(RequestContext context, String sessionId, String runId);

    EventHistoryResponse listEvents(RequestContext context, String sessionId, String runId, long afterSequence, int limit);

    PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit);

    PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit);

    PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit);

    RunResultResponse getRunResult(RequestContext context, String sessionId, String runId);
}
