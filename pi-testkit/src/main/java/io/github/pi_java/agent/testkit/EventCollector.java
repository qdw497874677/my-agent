package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventType;

import java.util.ArrayList;
import java.util.List;

public final class EventCollector implements EventSink {
    private final List<RunEvent> events = new ArrayList<>();

    @Override
    public void publish(RunEvent event) {
        events.add(event);
    }

    public List<RunEvent> events() {
        return List.copyOf(events);
    }

    public List<RunEventType> types() {
        return events.stream().map(RunEvent::type).toList();
    }

    public RunEvent last() {
        return events.get(events.size() - 1);
    }

    public void assertMonotonicSequences() {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).sequence() != i + 1L) {
                throw new AssertionError("event sequence must be monotonic starting at 1");
            }
        }
    }

    public void assertExactlyOneTerminalEventLast() {
        long terminalCount = events.stream().filter(event -> isTerminal(event.type())).count();
        if (terminalCount != 1) {
            throw new AssertionError("expected exactly one terminal event but found " + terminalCount);
        }
        if (!isTerminal(last().type())) {
            throw new AssertionError("terminal event must be last");
        }
    }

    public static boolean isTerminal(RunEventType type) {
        return type == RunEventType.RUN_COMPLETED
                || type == RunEventType.RUN_FAILED
                || type == RunEventType.RUN_CANCELLED
                || type == RunEventType.RUN_POLICY_BLOCKED;
    }
}
