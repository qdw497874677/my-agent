package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/runs")
public class RunController {

    private final RunCommandService runCommandService;
    private final RunQueryService runQueryService;
    private final RunActivationTrigger runActivationTrigger;

    public RunController(RunCommandService runCommandService, RunQueryService runQueryService, RunActivationTrigger runActivationTrigger) {
        this.runCommandService = runCommandService;
        this.runQueryService = runQueryService;
        this.runActivationTrigger = runActivationTrigger;
    }

    @PostMapping
    public ResponseEntity<RunResponse> createRun(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @RequestBody CreateRunRequest request) {
        RunResponse response = runCommandService.createRun(SessionController.toRequestContext(principal, servletRequest), sessionId, request);
        runActivationTrigger.triggerAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{runId}")
    public RunDetailResponse getRun(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId) {
        return runQueryService.getRunDetail(SessionController.toRequestContext(principal, servletRequest), sessionId, runId);
    }

    @GetMapping("/{runId}/status")
    public RunStatusResponse getRunStatus(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId) {
        return runQueryService.getRunStatus(SessionController.toRequestContext(principal, servletRequest), sessionId, runId);
    }

    @PostMapping("/{runId}/cancel")
    public RunStatusResponse cancelRun(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestBody CancelRunRequest request) {
        return runCommandService.cancelRun(SessionController.toRequestContext(principal, servletRequest), sessionId, runId, request);
    }

    @GetMapping("/{runId}/events")
    public EventHistoryResponse listEvents(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestParam(name = "afterSequence", defaultValue = "0") long afterSequence,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return runQueryService.listEvents(SessionController.toRequestContext(principal, servletRequest), sessionId, runId, afterSequence, limit);
    }

    @GetMapping("/{runId}/steps")
    public PageResponse<Map<String, Object>> listSteps(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return runQueryService.listSteps(SessionController.toRequestContext(principal, servletRequest), sessionId, runId, limit);
    }

    @GetMapping("/{runId}/messages")
    public PageResponse<Map<String, Object>> listMessages(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return runQueryService.listMessages(SessionController.toRequestContext(principal, servletRequest), sessionId, runId, limit);
    }

    @GetMapping("/{runId}/tool-calls")
    public PageResponse<Map<String, Object>> listToolCalls(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        return runQueryService.listToolCalls(SessionController.toRequestContext(principal, servletRequest), sessionId, runId, limit);
    }

    @GetMapping("/{runId}/result")
    public RunResultResponse getRunResult(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("runId") String runId) {
        return runQueryService.getRunResult(SessionController.toRequestContext(principal, servletRequest), sessionId, runId);
    }

    public interface RunActivationTrigger {
        void triggerAsync();
    }
}
