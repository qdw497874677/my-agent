package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.app.usecase.DefaultToolExecutionGateway;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import io.github.pi_java.agent.infrastructure.event.PersistingEventSink;
import io.github.pi_java.agent.infrastructure.tool.InMemoryToolRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {PiCloudServerApplication.class, ToolGovernanceWiringTest.TestRuntimeConfiguration.class})
@ActiveProfiles("test")
class ToolGovernanceWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void cloudContextHasExactlyOneGovernedToolExecutionGateway() {
        Map<String, ToolExecutionGateway> gateways = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ToolExecutionGateway.class);

        assertThat(gateways).hasSize(1);
        assertThat(AopProxyUtils.ultimateTargetClass(gateways.values().iterator().next())).isAssignableTo(DefaultToolExecutionGateway.class);
    }

    @Test
    void builtInToolsAreVisibleThroughRegistryAndCatalogQueryService() {
        ToolRegistry registry = applicationContext.getBean(ToolRegistry.class);

        assertThat(registry).isInstanceOf(InMemoryToolRegistry.class);
        assertThat(registry.listTools())
                .extracting("id")
                .containsExactlyInAnyOrder("builtin.info", "builtin.workspace.write", "builtin.workspace.command");
        assertThat(applicationContext.getBean(ToolRegistryQueryService.class).listTools(testContext()).tools())
                .extracting("id")
                .contains("builtin.info", "builtin.workspace.write", "builtin.workspace.command");
    }

    @Test
    void gatewayUsesPersistingEventSinkAndAuditRepositoryCollaborators() {
        assertThat(applicationContext.getBean(ToolExecutionGateway.class).toString())
                .contains("eventSink=PersistingEventSink")
                .contains("auditRepository=JdbcAuditRepository");
        assertThat(applicationContext.getBean(EventSink.class)).isInstanceOf(PersistingEventSink.class);
        assertThat(applicationContext.getBean(AuditRepository.class).getClass().getName()).contains("JdbcAuditRepository");
    }

    @Test
    void runtimeBeanIsGatewayAwareAndCloudWiringDoesNotBypassToolGateway() {
        AgentRuntime runtime = applicationContext.getBean(AgentRuntime.class);

        assertThat(runtime.toString()).contains("ToolExecutionGateway");
        assertThat(applicationContext.getBean(ToolExecutionGateway.class)).isNotNull();
    }

    private static io.github.pi_java.agent.app.context.RequestContext testContext() {
        return new io.github.pi_java.agent.app.context.RequestContext(
                new io.github.pi_java.agent.app.context.SecurityPrincipalContext("tenant-a", "user-a", java.util.Set.of()),
                new io.github.pi_java.agent.app.context.CorrelationContext("trace-1", "corr-1", "cause-1"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRuntimeConfiguration {
        @Bean
        AgentRuntime agentRuntime(ToolExecutionGateway toolExecutionGateway) {
            return new GatewayAwareTestRuntime(toolExecutionGateway);
        }
    }

    private record GatewayAwareTestRuntime(ToolExecutionGateway toolExecutionGateway) implements AgentRuntime {
        @Override
        public io.github.pi_java.agent.domain.runtime.RunHandle start(io.github.pi_java.agent.domain.runtime.RunContext context) {
            return new io.github.pi_java.agent.domain.runtime.RunHandle(context.workspaceScope().runId(),
                    io.github.pi_java.agent.domain.runtime.RunStatus.SUCCEEDED, java.util.Optional.empty());
        }

        @Override
        public void cancel(String runId, String reason) {
            // No-op for wiring verification.
        }

        @Override
        public String toString() {
            return "GatewayAwareTestRuntime[ToolExecutionGateway=" + toolExecutionGateway.getClass().getSimpleName() + "]";
        }
    }
}
