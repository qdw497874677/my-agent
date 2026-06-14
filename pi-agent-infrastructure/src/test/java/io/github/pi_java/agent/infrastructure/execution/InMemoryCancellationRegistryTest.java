package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.domain.runtime.CancellationToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCancellationRegistryTest {

    private final InMemoryCancellationRegistry registry = new InMemoryCancellationRegistry();

    @Test
    void tokenForRegistersActiveToken() {
        CancellationToken token = registry.tokenFor("run-1");

        assertThat(registry.activeToken("run-1")).containsSame(token);
    }

    @Test
    void requestCancellationSignalsOnlyActiveRun() {
        CancellationToken token = registry.tokenFor("run-1");

        assertThat(registry.requestCancellation("run-1", "user requested")).isTrue();
        assertThat(token.isCancellationRequested()).isTrue();
        assertThat(token.reason()).contains("user requested");
        assertThat(registry.requestCancellation("missing", "user requested")).isFalse();
    }

    @Test
    void removeDropsCancellationToken() {
        registry.tokenFor("run-1");

        registry.remove("run-1");

        assertThat(registry.activeToken("run-1")).isEmpty();
    }
}
