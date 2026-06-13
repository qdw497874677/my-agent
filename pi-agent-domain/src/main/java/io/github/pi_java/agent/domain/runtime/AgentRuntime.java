package io.github.pi_java.agent.domain.runtime;

public interface AgentRuntime {
    RunHandle start(RunContext context);

    void cancel(String runId, String reason);
}
