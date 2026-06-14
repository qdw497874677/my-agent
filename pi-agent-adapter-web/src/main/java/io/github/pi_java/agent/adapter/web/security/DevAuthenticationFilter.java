package io.github.pi_java.agent.adapter.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class DevAuthenticationFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Pi-Dev-Tenant";
    public static final String USER_HEADER = "X-Pi-Dev-User";
    static final String DISABLE_DEFAULTS_HEADER = "X-Pi-Dev-Disable-Defaults";

    private static final String DEFAULT_TENANT = "dev-tenant";
    private static final String DEFAULT_USER = "dev-user";

    private final boolean testProfile;

    public DevAuthenticationFilter(Environment environment) {
        this.testProfile = List.of(environment.getActiveProfiles()).contains("test");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromSafeDevHeaders(request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateFromSafeDevHeaders(HttpServletRequest request) {
        String tenantId = textOrNull(request.getHeader(TENANT_HEADER));
        String userId = textOrNull(request.getHeader(USER_HEADER));
        boolean defaultsDisabled = Boolean.parseBoolean(request.getHeader(DISABLE_DEFAULTS_HEADER));

        if (testProfile && !defaultsDisabled) {
            tenantId = tenantId == null ? DEFAULT_TENANT : tenantId;
            userId = userId == null ? DEFAULT_USER : userId;
        }

        if (tenantId == null || userId == null) {
            return;
        }

        PiPrincipal principal = new PiPrincipal(tenantId, userId, List.of("ROLE_DEV_USER"));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DEV_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
