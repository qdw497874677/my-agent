package io.github.pi_java.agent.infrastructure.execution;

import io.github.pi_java.agent.app.port.execution.RunDispatcher;

import java.util.Objects;
import java.util.concurrent.Executor;

public class RunWorkerScheduler {

    private final RunDispatcher runDispatcher;
    private final Executor executor;
    private final String workerId;

    public RunWorkerScheduler(RunDispatcher runDispatcher, Executor executor, String workerId) {
        this.runDispatcher = Objects.requireNonNull(runDispatcher, "runDispatcher must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        this.workerId = workerId;
    }

    public void pollOnce() {
        runDispatcher.dispatch(workerId);
    }

    public void triggerAsync() {
        executor.execute(this::pollOnce);
    }
}
