package io.github.pi_java.agent.domain.tool;

public enum ToolExecutionStatus {
    SUCCESS,
    FAILED,
    DENIED,
    APPROVAL_REQUIRED,
    SANDBOX_REQUIRED,
    CANCELLED,
    TIMED_OUT,
    PREVIEW_ONLY
}
