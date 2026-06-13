package io.github.pi_java.agent.domain.runtime;

public enum RunStatus {
    QUEUED,
    RUNNING,
    SUSPENDED,
    CANCELLING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    POLICY_BLOCKED
}
