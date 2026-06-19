package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunHandle;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.infrastructure.observability.TelemetryRunDispatcher;
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

import java.util.Optional;

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

    @Test
    void runDispatcherIsComposedThroughTelemetryDecorator() {
        RunDispatcher runDispatcher = applicationContext.getBean(RunDispatcher.class);

        assertThat(AopProxyUtils.ultimateTargetClass(runDispatcher)).isAssignableTo(TelemetryRunDispatcher.class);
        assertThat(runDispatcher).isInstanceOf(TelemetryRunDispatcher.class);
        assertThat(((TelemetryRunDispatcher) runDispatcher).delegate()).isNotNull();
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
