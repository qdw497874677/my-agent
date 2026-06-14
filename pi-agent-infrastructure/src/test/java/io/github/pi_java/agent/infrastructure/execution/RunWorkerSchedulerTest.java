package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RunWorkerSchedulerTest {

    @Test
    void pollOnceDispatchesQueuedRuns() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RunWorkerScheduler scheduler = new RunWorkerScheduler(dispatcher, Runnable::run, "worker-1");

        scheduler.pollOnce();

        assertThat(dispatcher.dispatchCount.get()).isEqualTo(1);
        assertThat(dispatcher.workerId.get()).isEqualTo("worker-1");
    }

    @Test
    void triggerAsyncDispatchesWithoutManualDispatcherCall() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        AtomicReference<Runnable> submitted = new AtomicReference<>();
        Executor executor = submitted::set;
        RunWorkerScheduler scheduler = new RunWorkerScheduler(dispatcher, executor, "worker-1");

        scheduler.triggerAsync();
        assertThat(dispatcher.dispatchCount.get()).isZero();
        submitted.get().run();

        assertThat(dispatcher.dispatchCount.get()).isEqualTo(1);
    }

    private static final class RecordingDispatcher implements RunDispatcher {
        private final AtomicInteger dispatchCount = new AtomicInteger();
        private final AtomicReference<String> workerId = new AtomicReference<>();

        @Override
        public void dispatch(String workerId) {
            dispatchCount.incrementAndGet();
            this.workerId.set(workerId);
        }

        @Override
        public void dispatchRun(String workerId, String runId) {
            dispatch(workerId);
        }
    }
}
