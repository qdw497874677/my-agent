package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.infrastructure.observability.PiTelemetry;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetryRedactor;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class)
@ActiveProfiles("test")
class ObservabilityConfigurationTest {

    @Autowired
    private PiTelemetry piTelemetry;

    @Autowired
    private PiTelemetryRedactor redactor;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Environment environment;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void providesPiTelemetryBeansWithoutExternalBackend() {
        assertThat(piTelemetry).isNotNull();
        assertThat(redactor).isNotNull();
        assertThat(meterRegistry).isNotNull();
        assertThat(piTelemetry.span("pi.test.noop").getSpanContext().isValid()).isFalse();
    }

    @Test
    void exposesPrometheusAndOtlpConfigurationHooks() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .contains("health", "info", "metrics", "prometheus");
        assertThat(environment.getProperty("management.otlp.metrics.export.url")).isEmpty();
        assertThat(environment.getProperty("management.otlp.logging.endpoint")).isEmpty();
        assertThat(PrometheusMetricsExportAutoConfiguration.class.getName()).contains("Prometheus");
    }
}
