package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.observability.OperationsMetricsReader;
import io.github.pi_java.agent.infrastructure.observability.MicrometerOperationsMetricsReader;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetry;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetryRedactor;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ObservabilityBeanConfiguration {

    @Bean
    PiTelemetryRedactor piTelemetryRedactor() {
        return new PiTelemetryRedactor();
    }

    @Bean
    Tracer piOpenTelemetryTracer() {
        return GlobalOpenTelemetry.getTracer("io.github.pi_java.agent");
    }

    @Bean
    PiTelemetry piTelemetry(MeterRegistry meterRegistry, Tracer piOpenTelemetryTracer) {
        return new PiTelemetry(meterRegistry, piOpenTelemetryTracer);
    }

    @Bean
    OperationsMetricsReader operationsMetricsReader(MeterRegistry meterRegistry, Clock clock,
                                                    PiTelemetryRedactor piTelemetryRedactor) {
        return new MicrometerOperationsMetricsReader(meterRegistry, clock, piTelemetryRedactor);
    }
}
