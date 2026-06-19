package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.tool.ToolExecutionGateway;
import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.infrastructure.observability.TelemetryToolExecutionGateway;
import io.github.pi_java.agent.infrastructure.observability.TelemetryToolPolicyEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PiCloudServerApplication.class)
@ActiveProfiles("test")
class ToolTelemetryWiringTest {

    @Autowired
    private ToolExecutionGateway toolExecutionGateway;

    @Autowired
    private ToolPolicyEvaluator toolPolicyEvaluator;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void tool_governance_beans_are_wrapped_with_telemetry_decorators() {
        assertThat(toolExecutionGateway).isInstanceOf(TelemetryToolExecutionGateway.class);
        assertThat(((TelemetryToolExecutionGateway) toolExecutionGateway).delegate().getClass().getSimpleName())
                .isEqualTo("DefaultToolExecutionGateway");
        assertThat(toolPolicyEvaluator).isInstanceOf(TelemetryToolPolicyEvaluator.class);
        assertThat(((TelemetryToolPolicyEvaluator) toolPolicyEvaluator).delegate().getClass().getSimpleName())
                .isEqualTo("DefaultToolPolicyEvaluator");
    }
}
