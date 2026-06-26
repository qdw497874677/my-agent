package io.github.pi_java.agent.adapter.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Profile({"dev", "test"})
    SecurityFilterChain apiSecurity(HttpSecurity http, Environment environment) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(vaadinPublicMatchers()).permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(new DevAuthenticationFilter(environment), UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localApiSecurity(HttpSecurity http, Environment environment) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new DevAuthenticationFilter(environment), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Profile("!dev & !test & !local")
    SecurityFilterChain productionApiSecurity(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(vaadinPublicMatchers()).permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Profile({"dev", "test", "local"})
    JwtDecoder devJwtDecoder() {
        return token -> {
            throw new JwtException("JWT decoding is not configured for dev/test authentication");
        };
    }

    private static String[] vaadinPublicMatchers() {
        return new String[] {
                "/",
                "/console", "/console/**",
                "/admin/governance", "/admin/governance/**",
                "/admin/governance/approvals", "/admin/governance/approvals/**",
                "/VAADIN/**",
                "/frontend/**",
                "/webjars/**",
                "/images/**",
                "/icons/**",
                "/themes/**",
                "/favicon.ico",
                "/manifest.webmanifest",
                "/sw.js",
                "/offline.html"
        };
    }
}
