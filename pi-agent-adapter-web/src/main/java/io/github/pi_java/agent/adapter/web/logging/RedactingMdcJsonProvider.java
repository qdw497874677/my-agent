package io.github.pi_java.agent.adapter.web.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetryRedactor;
import java.io.IOException;
import java.util.List;
import net.logstash.logback.composite.AbstractJsonProvider;

public final class RedactingMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    private static final List<String> MDC_KEYS = List.of("traceId", "correlationId", "tenantId", "userId", "sessionId", "runId");

    private final PiTelemetryRedactor redactor = new PiTelemetryRedactor();

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        for (String key : MDC_KEYS) {
            String value = event.getMDCPropertyMap().get(key);
            if (value != null && !value.isBlank()) {
                generator.writeStringField(key, redactor.safeTag(value));
            }
        }
    }
}
