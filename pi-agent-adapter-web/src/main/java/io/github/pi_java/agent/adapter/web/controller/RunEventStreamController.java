package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.adapter.web.sse.RunEventStreamService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class RunEventStreamController {

    private final RunEventStreamService runEventStreamService;

    public RunEventStreamController(RunEventStreamService runEventStreamService) {
        this.runEventStreamService = runEventStreamService;
    }

    @GetMapping(path = "/api/sessions/{sessionId}/runs/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRunEvents(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestParam(name = "afterSequence", defaultValue = "0") long afterSequence,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        return runEventStreamService.replayThenSubscribe(
                SessionController.toRequestContext(principal, servletRequest),
                sessionId,
                runId,
                afterSequence,
                lastEventId);
    }
}
