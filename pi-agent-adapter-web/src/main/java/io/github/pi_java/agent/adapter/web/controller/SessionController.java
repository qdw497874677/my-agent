package io.github.pi_java.agent.adapter.web.controller;

import io.github.pi_java.agent.adapter.web.correlation.CorrelationFilter;
import io.github.pi_java.agent.adapter.web.security.PiPrincipal;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionCommandService sessionCommandService;
    private final SessionQueryService sessionQueryService;

    public SessionController(SessionCommandService sessionCommandService, SessionQueryService sessionQueryService) {
        this.sessionCommandService = sessionCommandService;
        this.sessionQueryService = sessionQueryService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            Principal principal,
            HttpServletRequest servletRequest,
            @RequestBody CreateSessionRequest request) {
        return ResponseEntity.ok(sessionCommandService.createSession(toRequestContext(principal, servletRequest), request));
    }

    @GetMapping("/{sessionId}")
    public SessionResponse getSession(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId) {
        return sessionQueryService.getSession(toRequestContext(principal, servletRequest), sessionId);
    }

    @GetMapping("/{sessionId}/history")
    public SessionHistoryResponse getSessionHistory(
            Principal principal,
            HttpServletRequest servletRequest,
            @PathVariable("sessionId") String sessionId) {
        return sessionQueryService.getSessionHistory(toRequestContext(principal, servletRequest), sessionId);
    }

    static RequestContext toRequestContext(Principal principal, HttpServletRequest request) {
        return piPrincipal(principal).toRequestContext(
                attribute(request, CorrelationFilter.TRACE_ATTRIBUTE),
                attribute(request, CorrelationFilter.CORRELATION_ATTRIBUTE),
                attribute(request, CorrelationFilter.CAUSATION_ATTRIBUTE));
    }

    private static PiPrincipal piPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof PiPrincipal piPrincipal) {
            return piPrincipal;
        }
        throw new IllegalStateException("Authenticated PiPrincipal is required");
    }

    private static String attribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }
}
