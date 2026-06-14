package io.github.pi_java.agent.client.run;

import java.util.List;
import java.util.Map;

public record RunDetailResponse(
        RunResponse run,
        List<Map<String, Object>> events,
        List<Map<String, Object>> steps,
        List<Map<String, Object>> messages,
        List<Map<String, Object>> toolCalls,
        RunResultResponse result) {
}
