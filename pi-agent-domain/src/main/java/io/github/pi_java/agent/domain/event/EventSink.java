package io.github.pi_java.agent.domain.event;

public interface EventSink {
    void publish(RunEvent event);
}
