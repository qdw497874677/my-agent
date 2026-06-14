package io.github.pi_java.agent.app.port.execution;

public interface RunDispatcher {

    void dispatch(String workerId);

    void dispatchRun(String workerId, String runId);
}
