package io.github.pi_java.agent.adapter.web.correlation;

import io.github.pi_java.agent.adapter.web.security.PiPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String REQUEST_HEADER = "X-Request-ID";
    public static final String TRACE_ATTRIBUTE = "pi.traceId";
    public static final String CORRELATION_ATTRIBUTE = "pi.correlationId";
    public static final String CAUSATION_ATTRIBUTE = "pi.causationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = firstText(request.getHeader(CORRELATION_HEADER), request.getHeader(REQUEST_HEADER), UUID.randomUUID().toString());
        String traceId = UUID.randomUUID().toString();
        String causationId = firstText(request.getHeader("X-Causation-ID"), correlationId);

        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
        request.setAttribute(CAUSATION_ATTRIBUTE, causationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            MDC.put("traceId", traceId);
            MDC.put("correlationId", correlationId);
            putPrincipalMdc();
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("correlationId");
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    private static void putPrincipalMdc() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PiPrincipal principal) {
            MDC.put("tenantId", principal.tenantId());
            MDC.put("userId", principal.userId());
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return UUID.randomUUID().toString();
    }
}
