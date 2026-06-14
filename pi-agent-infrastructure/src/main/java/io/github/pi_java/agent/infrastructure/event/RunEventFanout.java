package io.github.pi_java.agent.infrastructure.event;

import io.github.pi_java.agent.domain.event.RunEvent;

public interface RunEventFanout {
    void publish(RunEvent event);
}
