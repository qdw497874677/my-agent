package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.github.pi_java.agent.adapter.web.logging.RedactingMdcJsonProvider;
import io.github.pi_java.agent.adapter.web.logging.RedactingMessageJsonProvider;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class StructuredLoggingRedactionTest {

    private static final String FAKE_SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void jsonLogsContainCorrelationMdcAndRedactFakeSecrets() {
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("correlationId", "corr-123");
        MDC.put("tenantId", "tenant-a");
        MDC.put("userId", "user-a");
        MDC.put("sessionId", "session-a");
        MDC.put("runId", "run-a");
        try {
            LoggingEvent event = new LoggingEvent();
            event.setLoggerName(getClass().getName());
            event.setLevel(Level.INFO);
            event.setThreadName("test-thread");
            event.setMessage("attempted leak " + FAKE_SECRET);
            event.setMDCPropertyMap(MDC.getCopyOfContextMap());

            String json = encode(event);

            assertThat(json).contains("\"traceId\":\"4bf92f3577b34da6a3ce929d0e0e4736\"");
            assertThat(json).contains("\"correlationId\":\"corr-123\"");
            assertThat(json).contains("\"tenantId\":\"tenant-a\"");
            assertThat(json).contains("\"userId\":\"user-a\"");
            assertThat(json).contains("\"sessionId\":\"session-a\"");
            assertThat(json).contains("\"runId\":\"run-a\"");
            assertThat(json).contains("[REDACTED]");
            assertThat(json).doesNotContain(FAKE_SECRET);
        } finally {
            MDC.clear();
        }
    }

    private static String encode(LoggingEvent event) {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addTimestamp(null);
        providers.addLogLevel(null);
        providers.addLoggerName(null);
        providers.addThreadName(null);
        providers.addProvider(new RedactingMessageJsonProvider());
        providers.addProvider(new RedactingMdcJsonProvider());
        encoder.setProviders(providers);
        encoder.start();
        try {
            return new String(encoder.encode(event), StandardCharsets.UTF_8);
        } finally {
            encoder.stop();
        }
    }
}
