package io.github.pi_java.agent.testkit;

import java.time.Instant;

public final class DeterministicClock {
    private Instant current;

    public DeterministicClock(Instant start) {
        this.current = start;
    }

    public Instant nextInstant() {
        Instant next = current;
        current = current.plusSeconds(1);
        return next;
    }

    public Instant peek() {
        return current;
    }
}
