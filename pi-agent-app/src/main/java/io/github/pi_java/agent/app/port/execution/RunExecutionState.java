package io.github.pi_java.agent.app.port.execution;

public enum RunExecutionState {
    QUEUED,
    CLAIMED,
    RUNNING,
    CANCEL_REQUESTED,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMED_OUT;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == TIMED_OUT;
    }
}
