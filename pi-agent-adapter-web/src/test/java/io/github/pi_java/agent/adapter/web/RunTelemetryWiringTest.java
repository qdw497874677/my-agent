package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetryNames;
import io.github.pi_java.agent.infrastructure.observability.RunEventTelemetrySink;
import io.github.pi_java.agent.infrastructure.observability.TelemetryRunDispatcher;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PiCloudServerApplication.class, RunTelemetryWiringTest.TestRuntimeConfiguration.class})
@ActiveProfiles("test")
class RunTelemetryWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void runDispatcherIsComposedThroughTelemetryDecorator() {
        RunDispatcher runDispatcher = applicationContext.getBean(RunDispatcher.class);

        assertThat(AopProxyUtils.ultimateTargetClass(runDispatcher)).isAssignableTo(TelemetryRunDispatcher.class);
        assertThat(runDispatcher).isInstanceOf(TelemetryRunDispatcher.class);
        assertThat(((TelemetryRunDispatcher) runDispatcher).delegate()).isNotNull();
    }

    @Test
    void eventSinkIsComposedThroughRunEventTelemetryDecorator() {
        EventSink eventSink = applicationContext.getBean(EventSink.class);

        assertThat(eventSink).isInstanceOf(RunEventTelemetrySink.class);

        eventSink.publish(sampleRunEvent());

        assertThat(meterRegistry.find(PiTelemetryNames.RUN_EVENTS_TOTAL).counter())
                .isNotNull()
                .extracting(counter -> counter.count())
                .isEqualTo(1.0d);
    }

    private static RunEvent sampleRunEvent() {
        return new RunEvent(
                "event-run-created",
                new TenantId("tenant-a"),
                new UserId("user-a"),
                new SessionId("session-a"),
                new RunId("run-a"),
                new StepId("step-a"),
                new WorkspaceId("workspace-a"),
                1,
                Instant.parse("2026-06-19T00:00:00Z"),
                RunEventType.RUN_CREATED,
                new TraceId("4bf92f3577b34da6a3ce929d0e0e4736"),
                new CorrelationId("corr-a"),
                new CausationId("cause-a"),
                new RunEventPayload.RunLifecyclePayload(RunStatus.QUEUED, null),
                EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRuntimeConfiguration {

        @Bean
        AgentRuntime agentRuntime() {
            return new AgentRuntime() {
                @Override
                public RunHandle start(RunContext context) {
                    return new RunHandle(context.sessionContext().workspaceScope().orElseThrow().runId(),
                            RunStatus.SUCCEEDED, Optional.empty());
                }

                @Override
                public void cancel(String runId, String reason) {
                    // No-op for wiring verification.
                }
            };
        }
    }
}
