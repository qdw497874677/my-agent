package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;

public interface RunCommandService {

    RunResponse createRun(RequestContext context, String sessionId, CreateRunRequest request);

    RunStatusResponse cancelRun(RequestContext context, String sessionId, String runId, CancelRunRequest request);
}
