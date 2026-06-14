package io.github.pi_java.agent.domain.model;

public enum ModelFinishReason {
    STOP,
    LENGTH,
    TOOL_CALLS,
    CONTENT_FILTER,
    CANCELLED,
    TIMEOUT,
    ERROR,
    UNKNOWN
}
