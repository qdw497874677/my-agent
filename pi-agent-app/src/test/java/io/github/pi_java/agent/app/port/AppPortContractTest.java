package io.github.pi_java.agent.app.port;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.execution.CancellationRegistry;
import io.github.pi_java.agent.app.port.execution.QueuedRun;
import io.github.pi_java.agent.app.port.execution.RunExecutionState;
import io.github.pi_java.agent.app.port.execution.RunQueue;
import io.github.pi_java.agent.app.port.execution.RunTerminalEventPublisher;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AppPortContractTest {

    @Test
    void queuePortIsBrokerNeutral() throws NoSuchMethodException {
        assertThat(RunQueue.class.getMethod("enqueue", QueuedRun.class).getReturnType()).isEqualTo(void.class);
        assertThat(RunQueue.class.getMethod("claimNext", String.class, Instant.class).getGenericReturnType().getTypeName())
                .isEqualTo("java.util.Optional<io.github.pi_java.agent.app.port.execution.QueuedRun>");
        assertThat(RunQueue.class.getMethod("markRunning", String.class, Instant.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(RunQueue.class.getMethod("markTerminal", String.class, String.class, Instant.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(RunQueue.class.getMethod("removeIfTerminal", String.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(importsOf(RunQueue.class)).doesNotContain("postgres", "Redis", "Kafka", "Rabbit", "Jdbc", "DataSource", "Connection", "SseEmitter");
    }

    @Test
    void queuedCancellationReturnsQueuedRunForTerminalEventPublishing() throws NoSuchMethodException {
        Method cancelQueued = RunQueue.class.getMethod("cancelQueuedAndReturn", String.class, String.class, Instant.class);
        assertThat(cancelQueued.getGenericReturnType().getTypeName())
                .isEqualTo("java.util.Optional<io.github.pi_java.agent.app.port.execution.QueuedRun>");
        assertThat(Arrays.stream(RunQueue.class.getMethods()).map(Method::getName))
                .doesNotContain("cancelQueued");

        QueuedRun queuedRun = new QueuedRun(
                "run-1", "session-1", "tenant-1", "user-1", "workspace-1", "trace-1", "corr-1",
                "task", Map.of("prompt", "hello"), Instant.parse("2026-06-14T00:00:00Z"), 0);
        assertThat(queuedRun.runId()).isEqualTo("run-1");
        assertThat(queuedRun.input()).containsEntry("prompt", "hello");
    }

    @Test
    void persistencePortsExposeIdempotentTerminalContracts() throws NoSuchMethodException {
        assertThat(RunEventStore.class.getMethod("hasTerminalEvent", String.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(RunProjectionRepository.class.getMethod("requestCancellation", String.class, String.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(RunProjectionRepository.class.getMethod(
                "markTerminalIfNotTerminal", String.class, String.class, Map.class, Map.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(RunProjectionRepository.class.getMethod("getStatus", RequestContext.class, String.class, String.class).getReturnType())
                .isEqualTo(RunStatusResponse.class);
    }

    @Test
    void terminalEventPublisherRequiresHasTerminalEventGuard() throws NoSuchMethodException {
        assertThat(RunTerminalEventPublisher.class.getMethod("publishCompletedIfAbsent", QueuedRun.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(RunTerminalEventPublisher.class.getMethod("publishCancelledIfAbsent", QueuedRun.class, String.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(RunTerminalEventPublisher.class.getMethod("publishFailedIfAbsent", QueuedRun.class, String.class, String.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(RunTerminalEventPublisher.class.getMethod("publishTimedOutIfAbsent", QueuedRun.class, String.class, Instant.class).getReturnType())
                .isEqualTo(boolean.class);
    }

    @Test
    void cancellationRegistrySeparatesDurableAndRuntimeCancellation() throws NoSuchMethodException {
        assertThat(CancellationRegistry.class.getMethod("tokenFor", String.class).getReturnType()).isEqualTo(CancellationToken.class);
        assertThat(CancellationRegistry.class.getMethod("activeToken", String.class).getGenericReturnType().getTypeName())
                .isEqualTo("java.util.Optional<io.github.pi_java.agent.domain.runtime.CancellationToken>");
        assertThat(CancellationRegistry.class.getMethod("requestCancellation", String.class, String.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(CancellationRegistry.class.getMethod("remove", String.class).getReturnType()).isEqualTo(void.class);

        assertThat(RunExecutionState.QUEUED.terminal()).isFalse();
        assertThat(RunExecutionState.RUNNING.terminal()).isFalse();
        assertThat(RunExecutionState.CANCELLED.terminal()).isTrue();
        assertThat(RunExecutionState.TIMED_OUT.terminal()).isTrue();
    }

    @Test
    void appPortsDoNotImportInfrastructureTypes() {
        Set<String> portImports = Set.of(
                importsOf(RunQueue.class),
                importsOf(CancellationRegistry.class),
                importsOf(RunTerminalEventPublisher.class),
                importsOf(RunEventStore.class),
                importsOf(RunProjectionRepository.class)
        );

        assertThat(portImports).allSatisfy(imports ->
                assertThat(imports).doesNotContain("postgres", "Redis", "Kafka", "Rabbit", "Jdbc", "DataSource", "Connection", "SseEmitter"));
    }

    private static String importsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .map(Class::getName)
                .collect(Collectors.joining("\n"));
    }
}
