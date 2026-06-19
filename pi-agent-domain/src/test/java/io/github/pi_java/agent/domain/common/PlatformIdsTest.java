package io.github.pi_java.agent.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import org.junit.jupiter.api.Test;

class PlatformIdsTest {

    @Test
    void traceIdAcceptsW3cLowercaseHex() {
        TraceId traceId = new TraceId("0123456789abcdef0123456789abcdef");

        assertThat(traceId.value()).isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    void traceIdRejectsInvalidW3cValues() {
        assertThatThrownBy(() -> new TraceId("00000000000000000000000000000000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceId("123e4567-e89b-12d3-a456-426614174000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceId("0123456789ABCDEF0123456789ABCDEF"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceId(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void traceIdMigratesLegacyUuidDeterministically() {
        TraceId traceId = TraceId.fromLegacyUuid("123e4567-e89b-12d3-a456-426614174000");

        assertThat(traceId.value()).isEqualTo("123e4567e89b12d3a456426614174000");
    }

    @Test
    void newRandomCreatesW3cTraceId() {
        TraceId traceId = TraceId.newRandom();

        assertThat(traceId.value()).matches("[0-9a-f]{32}");
    }
}
