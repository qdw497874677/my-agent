package io.github.pi_java.agent.adapter.web.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetryRedactor;
import java.io.IOException;
import net.logstash.logback.composite.AbstractJsonProvider;

public final class RedactingMessageJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    private final PiTelemetryRedactor redactor = new PiTelemetryRedactor();

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String message = event.getFormattedMessage();
        generator.writeStringField("message", redactor.isSensitive(message) ? PiTelemetryRedactor.REDACTED : message);
    }
}
