package io.github.pi_java.agent.app.port.observability;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.admin.OperationsSummaryResponse;

public interface OperationsMetricsReader {

    OperationsSummaryResponse summarize(RequestContext context);
}
