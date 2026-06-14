package io.github.pi_java.agent.adapter.web;

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
import io.github.pi_java.agent.app.usecase.DefaultSessionCommandService;
import io.github.pi_java.agent.app.usecase.DefaultSessionQueryService;
import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.infrastructure.event.PersistingEventSink;
import io.github.pi_java.agent.infrastructure.event.RunEventFanout;
import io.github.pi_java.agent.infrastructure.execution.DefaultRunDispatcher;
import io.github.pi_java.agent.infrastructure.execution.InMemoryCancellationRegistry;
import io.github.pi_java.agent.infrastructure.execution.RunWorkerScheduler;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcAuditRepository;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcRunEventStore;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcRunProjectionRepository;
import io.github.pi_java.agent.infrastructure.jdbc.JdbcSessionRepository;
import io.github.pi_java.agent.infrastructure.queue.PostgresRunQueue;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PiCloudServerApplication.class, CloudRuntimeWiringIntegrationTest.TestRuntimeConfiguration.class})
@ActiveProfiles("test")
class CloudRuntimeWiringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private RunWorkerScheduler runWorkerScheduler;

    @Test
    void exactlyOneRuntimeBeanOfEachCriticalType() {
        assertSingleBean(RunQueue.class, PostgresRunQueue.class);
        assertSingleBean(CancellationRegistry.class, InMemoryCancellationRegistry.class);
        assertSingleBean(RunDispatcher.class, DefaultRunDispatcher.class);
        assertSingleBean(RunWorkerScheduler.class, RunWorkerScheduler.class);
        assertSingleBean(EventSink.class, PersistingEventSink.class);
        assertSingleBean(RunEventFanout.class, SseRunEventFanout.class);
    }

    @Test
    void springContextWiresControllerToAppJdbcDispatcherRuntimeEventSinkAndSseFanout() {
        assertSingleBean(SessionCommandService.class, DefaultSessionCommandService.class);
        assertSingleBean(SessionQueryService.class, DefaultSessionQueryService.class);
        assertSingleBean(RunCommandService.class, DefaultRunCommandService.class);
        assertSingleBean(RunQueryService.class, DefaultRunQueryService.class);
        assertSingleBean(RunEventStore.class, JdbcRunEventStore.class);
        assertSingleBean(RunProjectionRepository.class, JdbcRunProjectionRepository.class);
        assertSingleBean(SessionRepository.class, JdbcSessionRepository.class);
        assertSingleBean(AuditRepository.class, JdbcAuditRepository.class);
        assertSingleBean(RunTerminalEventPublisher.class, RunTerminalEventPublisher.class);

        assertThat(applicationContext.getBean(RunController.class)).isNotNull();
        assertThat(applicationContext.getBean(RunDispatcher.class)).isInstanceOf(DefaultRunDispatcher.class);
        assertThat(applicationContext.getBean(EventSink.class)).isInstanceOf(PersistingEventSink.class);
        assertThat(applicationContext.getBean(RunEventFanout.class)).isInstanceOf(SseRunEventFanout.class);
    }

    @Test
    void eventSinkBeanPersistsBeforeSseFanout() {
        EventSink eventSink = applicationContext.getBean(EventSink.class);
        RunEventFanout fanout = applicationContext.getBean(RunEventFanout.class);

        assertThat(eventSink).isInstanceOf(PersistingEventSink.class);
        assertThat(fanout).isInstanceOf(SseRunEventFanout.class);
    }

    @Test
    void createRunTriggersWorkerActivationWithoutManualDispatcherCall() throws Exception {
        applicationContext.getBean(RunController.RunActivationTrigger.class).triggerAsync();
        verify(runWorkerScheduler).triggerAsync();
    }

    private <T> void assertSingleBean(Class<T> beanType, Class<?> implementationType) {
        Map<String, T> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, beanType);
        assertThat(beans).hasSize(1);
        assertThat(AopProxyUtils.ultimateTargetClass(beans.values().iterator().next())).isAssignableTo(implementationType);
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRuntimeConfiguration {

        @Bean
        AgentRuntime agentRuntime() {
            return new RecordingAgentRuntime();
        }
    }

    static final class RecordingAgentRuntime implements AgentRuntime {
        @Override
        public io.github.pi_java.agent.domain.runtime.RunHandle start(RunContext context) {
            return new io.github.pi_java.agent.domain.runtime.RunHandle(
                    context.sessionContext().workspaceScope().orElseThrow().runId(),
                    io.github.pi_java.agent.domain.runtime.RunStatus.SUCCEEDED,
                    java.util.Optional.empty());
        }

        @Override
        public void cancel(String runId, String reason) {
            // No-op for wiring verification.
        }
    }
}
