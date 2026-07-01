package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.adapter.web.controller.RunController;
import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.app.port.persistence.SessionRepository;
import io.github.pi_java.agent.app.usecase.DefaultRunCommandService;
import io.github.pi_java.agent.app.usecase.DefaultRunQueryService;
import io.github.pi_java.agent.app.usecase.DefaultRunTerminalEventPublisher;
import io.github.pi_java.agent.app.usecase.DefaultSessionCommandService;
import io.github.pi_java.agent.app.usecase.DefaultSessionQueryService;
import io.github.pi_java.agent.app.usecase.ConversationContextAssembler;
import io.github.pi_java.agent.app.usecase.ConversationContextPolicy;
import io.github.pi_java.agent.app.usecase.ConversationQueryService;
import io.github.pi_java.agent.app.usecase.ConversationTranscriptAssembler;
import io.github.pi_java.agent.app.usecase.DefaultConversationQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.infrastructure.event.PersistingEventSink;
import io.github.pi_java.agent.infrastructure.event.RunEventFanout;
import io.github.pi_java.agent.infrastructure.execution.DefaultRunDispatcher;
import io.github.pi_java.agent.infrastructure.execution.InMemoryCancellationRegistry;
import io.github.pi_java.agent.infrastructure.execution.RunWorkerScheduler;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcAuditRepository;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcRunEventStore;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcRunProjectionRepository;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcSessionRepository;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetry;
import io.github.pi_java.agent.infrastructure.observability.RunEventTelemetrySink;
import io.github.pi_java.agent.infrastructure.observability.TelemetryRunDispatcher;
import io.github.pi_java.agent.infrastructure.queue.PostgresRunQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class CloudRuntimeBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!local")
    SessionRepository sessionRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcSessionRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!local")
    AuditRepository auditRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcAuditRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!local")
    RunEventStore runEventStore(JdbcTemplate jdbcTemplate) {
        return new JdbcRunEventStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!local")
    RunProjectionRepository runProjectionRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRunProjectionRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!local")
    RunQueue runQueue(JdbcTemplate jdbcTemplate) {
        return new PostgresRunQueue(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    CancellationRegistry cancellationRegistry() {
        return new InMemoryCancellationRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    SseRunEventFanout runEventFanout() {
        return new SseRunEventFanout();
    }

    @Bean
    EventSink eventSink(
            TransactionTemplate transactionTemplate,
            RunEventStore runEventStore,
            RunProjectionRepository runProjectionRepository,
            RunEventFanout runEventFanout,
            ObjectProvider<PiTelemetry> piTelemetry) {
        EventSink persistingEventSink = new PersistingEventSink(transactionTemplate, runEventStore, runProjectionRepository, runEventFanout);
        PiTelemetry telemetry = piTelemetry.getIfAvailable();
        return telemetry == null ? persistingEventSink : new RunEventTelemetrySink(persistingEventSink, telemetry);
    }

    @Bean
    RunTerminalEventPublisher runTerminalEventPublisher(
            RunEventStore runEventStore,
            EventSink eventSink,
            Clock clock) {
        return new DefaultRunTerminalEventPublisher(runEventStore, eventSink, clock, () -> UUID.randomUUID().toString());
    }

    @Bean
    SessionCommandService sessionCommandService(
            SessionRepository sessionRepository,
            AuditRepository auditRepository,
            Clock clock) {
        return new DefaultSessionCommandService(sessionRepository, auditRepository, () -> UUID.randomUUID().toString(), clock);
    }

    @Bean
    SessionQueryService sessionQueryService(SessionRepository sessionRepository) {
        return new DefaultSessionQueryService(sessionRepository);
    }

    @Bean
    RunCommandService runCommandService(
            RunProjectionRepository runProjectionRepository,
            RunQueue runQueue,
            CancellationRegistry cancellationRegistry,
            RunTerminalEventPublisher runTerminalEventPublisher,
            AuditRepository auditRepository,
            Clock clock) {
        return new DefaultRunCommandService(runProjectionRepository, runQueue, cancellationRegistry,
                runTerminalEventPublisher, auditRepository, () -> UUID.randomUUID().toString(), clock);
    }

    @Bean
    RunQueryService runQueryService(RunProjectionRepository runProjectionRepository, RunEventStore runEventStore) {
        return new DefaultRunQueryService(runProjectionRepository, runEventStore);
    }

    @Bean
    ConversationTranscriptAssembler conversationTranscriptAssembler() {
        return new ConversationTranscriptAssembler();
    }

    @Bean
    ConversationQueryService conversationQueryService(
            SessionRepository sessionRepository,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            ConversationTranscriptAssembler assembler) {
        return new DefaultConversationQueryService(sessionRepository, runProjectionRepository, runEventStore, assembler);
    }

    @Bean
    ConversationContextPolicy conversationContextPolicy() {
        return ConversationContextPolicy.defaults();
    }

    @Bean
    ConversationContextAssembler conversationContextAssembler(
            ConversationQueryService conversationQueryService,
            ConversationContextPolicy conversationContextPolicy) {
        return new ConversationContextAssembler(conversationQueryService, conversationContextPolicy);
    }

    @Bean
    RunDispatcher runDispatcher(
            RunQueue runQueue,
            RunProjectionRepository runProjectionRepository,
            RunEventStore runEventStore,
            RunTerminalEventPublisher runTerminalEventPublisher,
            CancellationRegistry cancellationRegistry,
            AuditRepository auditRepository,
            AgentRuntime agentRuntime,
            Clock clock,
            ConversationContextAssembler conversationContextAssembler,
            ConversationContextPolicy conversationContextPolicy,
            ObjectProvider<PiTelemetry> piTelemetry,
            @Value("${pi.runtime.run-timeout-ms:30000}") long runTimeoutMs,
            @Value("${pi.runtime.default-model-ref:openai-compatible:${pi.providers.openai-compatible.default-model-id:gpt-4.1-mini}}") String defaultModelRef) {
        RunDispatcher defaultDispatcher = new DefaultRunDispatcher(runQueue, runProjectionRepository, runEventStore, runTerminalEventPublisher,
                cancellationRegistry, auditRepository, agentRuntime, clock, Duration.ofMillis(runTimeoutMs), defaultModelRef,
                conversationContextAssembler, conversationContextPolicy);
        PiTelemetry telemetry = piTelemetry.getIfAvailable();
        return telemetry == null ? defaultDispatcher : new TelemetryRunDispatcher(defaultDispatcher, telemetry);
    }

    @Bean
    RunWorkerScheduler runWorkerScheduler(
            RunDispatcher runDispatcher,
            @Qualifier("runWorkerExecutor")
            Executor runWorkerExecutor,
            @Value("${pi.runtime.worker.id:cloud-worker-1}") String workerId) {
        return new RunWorkerScheduler(runDispatcher, runWorkerExecutor, workerId);
    }

    @Bean
    Executor runWorkerExecutor() {
        return command -> Thread.ofVirtual().name("pi-run-worker-", 0).start(command);
    }

    @Bean
    RunController.RunActivationTrigger runActivationTrigger(RunWorkerScheduler runWorkerScheduler) {
        return runWorkerScheduler::triggerAsync;
    }

    @Bean
    ScheduledRunWorkerPoller scheduledRunWorkerPoller(RunWorkerScheduler runWorkerScheduler) {
        return new ScheduledRunWorkerPoller(runWorkerScheduler);
    }

    public static final class ScheduledRunWorkerPoller {
        private final RunWorkerScheduler runWorkerScheduler;

        private ScheduledRunWorkerPoller(RunWorkerScheduler runWorkerScheduler) {
            this.runWorkerScheduler = runWorkerScheduler;
        }

        @Scheduled(fixedDelayString = "${pi.runtime.worker.poll-delay-ms:250}")
        public void pollOnce() {
            runWorkerScheduler.pollOnce();
        }
    }
}
