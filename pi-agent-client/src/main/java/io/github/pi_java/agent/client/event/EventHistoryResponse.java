package io.github.pi_java.agent.client.event;

import java.util.List;

public record EventHistoryResponse(
        String sessionId,
        String runId,
        List<RunEventDto> events,
        long afterSequence,
        long nextAfterSequence,
        boolean terminal
) {
}
