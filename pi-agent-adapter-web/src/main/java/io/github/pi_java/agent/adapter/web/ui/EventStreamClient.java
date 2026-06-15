package io.github.pi_java.agent.adapter.web.ui;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Browser-facing SSE connection metadata for existing public run stream endpoints.
 */
public class EventStreamClient {

    public ConnectionSpec runEventStream(String sessionId, String runId, long afterSequence) {
        String url = "/api/sessions/" + segment(sessionId)
                + "/runs/" + segment(runId)
                + "/stream?afterSequence=" + Math.max(0, afterSequence);
        return new ConnectionSpec(url, true);
    }

    private static String segment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Path segment must not be blank");
        }
        return URLEncoder.encode(raw.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record ConnectionSpec(String url, boolean withCredentials) {

        public String eventSourceExpression() {
            return "new EventSource('" + url + "', { withCredentials: " + withCredentials + " })";
        }
    }
}
