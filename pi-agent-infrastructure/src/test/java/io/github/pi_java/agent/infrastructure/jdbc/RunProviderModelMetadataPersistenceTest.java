package io.github.pi_java.agent.infrastructure.jdbc;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.app.usecase.ConversationRunView;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunProviderMetadata;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RunProviderModelMetadataPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private JdbcRunProjectionRepository runProjectionRepository;
    private JdbcSessionRepository sessionRepository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        sessionRepository = new JdbcSessionRepository(jdbcTemplate);
        runProjectionRepository = new JdbcRunProjectionRepository(jdbcTemplate);
    }

    @Test
    void createFindAndListPersistSafeProviderModelFallbackMetadata() {
        RequestContext owner = context("tenant-a", "user-a");
        sessionRepository.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a",
                Instant.parse("2026-07-04T00:00:00Z"));

        runProjectionRepository.createRun(owner, "sess-a", "run-a", new CreateRunRequest(
                "agent", "chat", Map.of("text", "hello"), "ws", Map.of(
                "requestedModelRef", "local:gpt-4o-mini",
                "selectedModelRef", "openai:gpt-4o-mini",
                "resolvedProviderId", "openai",
                "resolvedModelId", "gpt-4o-mini",
                "fallbackMode", "LOCAL_PROFILE_FALLBACK",
                "readinessState", "READY",
                "safeErrorSummary", "fallback provider selected")));

        RunResponse found = runProjectionRepository.findRun(owner, "sess-a", "run-a").orElseThrow();
        assertThat(found.providerMetadata()).isEqualTo(new RunProviderMetadata(
                "local:gpt-4o-mini",
                "openai:gpt-4o-mini",
                "openai",
                "gpt-4o-mini",
                "LOCAL_PROFILE_FALLBACK",
                "READY",
                "fallback provider selected"));

        PageResponse<ConversationRunView> listed = runProjectionRepository.listRunsBySession(owner, "sess-a", 10, null);
        assertThat(listed.items()).hasSize(1);
        assertThat(listed.items().get(0).providerMetadata()).isEqualTo(found.providerMetadata());
    }

    @Test
    void providerMetadataPreservesTenantUserSessionFilters() {
        RequestContext owner = context("tenant-a", "user-a");
        RequestContext foreign = context("tenant-b", "user-b");
        sessionRepository.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a",
                Instant.parse("2026-07-04T00:00:00Z"));
        sessionRepository.create(foreign, new CreateSessionRequest("ws", Map.of()), "sess-b",
                Instant.parse("2026-07-04T00:00:00Z"));
        runProjectionRepository.createRun(owner, "sess-a", "run-a", new CreateRunRequest(
                "agent", "chat", Map.of("text", "owner"), "ws", Map.of("resolvedProviderId", "openai")));
        runProjectionRepository.createRun(foreign, "sess-b", "run-b", new CreateRunRequest(
                "agent", "chat", Map.of("text", "foreign"), "ws", Map.of("resolvedProviderId", "anthropic")));

        assertThat(runProjectionRepository.findRun(owner, "sess-a", "run-a")).isPresent();
        assertThat(runProjectionRepository.findRun(owner, "sess-b", "run-b")).isEmpty();
        assertThat(runProjectionRepository.listRunsBySession(owner, "sess-a", 10, null).items())
                .extracting(view -> view.providerMetadata().resolvedProviderId())
                .containsExactly("openai");
    }

    @Test
    void providerMetadataRedactsSecretsAndProviderConfigSnapshots() {
        RequestContext owner = context("tenant-a", "user-a");
        sessionRepository.create(owner, new CreateSessionRequest("ws", Map.of()), "sess-a",
                Instant.parse("2026-07-04T00:00:00Z"));

        runProjectionRepository.createRun(owner, "sess-a", "run-secret", new CreateRunRequest(
                "agent", "chat", Map.of("text", "hello"), "ws", Map.of(
                "requestedModelRef", "local:gpt-4o-mini",
                "resolvedProviderId", "openai",
                "apiKey", "sk-live-secret",
                "Authorization", "Bearer secret-token",
                "providerConfig", Map.of("baseUrl", "https://example.test", "apiKey", "sk-config-secret"),
                "safeErrorSummary", "401 Bearer secret-token apiKey=sk-live-secret")));

        RunResponse found = runProjectionRepository.findRun(owner, "sess-a", "run-secret").orElseThrow();
        assertThat(found.providerMetadata().resolvedProviderId()).isEqualTo("openai");
        assertThat(found.providerMetadata().safeErrorSummary()).doesNotContain("secret-token", "sk-live-secret");

        String storedJson = jdbcTemplate.queryForObject(
                "SELECT provider_metadata::text FROM runs WHERE run_id = ?", String.class, "run-secret");
        assertThat(storedJson).doesNotContain("apiKey", "Authorization", "providerConfig", "secret-token", "sk-live-secret");
    }

    private static RequestContext context(String tenantId, String userId) {
        return new RequestContext(
                new SecurityPrincipalContext(tenantId, userId, Set.of()),
                new CorrelationContext("0123456789abcdef0123456789abcdef", "correlation-" + tenantId, "causation-" + tenantId));
    }
}
