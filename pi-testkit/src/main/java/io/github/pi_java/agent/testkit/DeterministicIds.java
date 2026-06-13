package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.common.IdGenerator;

import java.util.HashMap;
import java.util.Map;

public final class DeterministicIds implements IdGenerator {
    private final Map<String, Integer> counters = new HashMap<>();

    @Override
    public String nextId(String prefix) {
        int next = counters.merge(prefix, 1, Integer::sum);
        return prefix + "-" + next;
    }
}
