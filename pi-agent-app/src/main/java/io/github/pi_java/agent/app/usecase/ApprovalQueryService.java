package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.approval.ApprovalSummaryDto;
import java.util.List;

public interface ApprovalQueryService {
    List<ApprovalSummaryDto> listPendingApprovals(RequestContext context, String sessionId, String runId);
}
