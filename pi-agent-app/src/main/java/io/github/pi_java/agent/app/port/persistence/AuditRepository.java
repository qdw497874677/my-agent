package io.github.pi_java.agent.app.port.persistence;

import io.github.pi_java.agent.app.context.RequestContext;

import java.util.Map;

public interface AuditRepository {

    void record(
            RequestContext context,
            String action,
            String resourceType,
            String resourceId,
            String sessionId,
            String runId,
            Map<String, Object> details);
}
