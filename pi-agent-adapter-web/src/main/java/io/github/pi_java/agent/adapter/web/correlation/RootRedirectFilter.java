package io.github.pi_java.agent.adapter.web.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RootRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPlainRootGet(request)) {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", request.getContextPath() + "/console");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isPlainRootGet(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && "/".equals(request.getRequestURI())
                && (request.getQueryString() == null || request.getQueryString().isBlank());
    }
}
