package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ToolCall;

public final class FakePolicy {
    private PolicyDecision decision;

    public FakePolicy(PolicyDecision decision) {
        this.decision = decision;
    }

    public static FakePolicy allow() {
        return new FakePolicy(PolicyDecision.ALLOW);
    }

    public FakePolicy decision(PolicyDecision decision) {
        this.decision = decision;
        return this;
    }

    public PolicyDecision decide(ToolCall toolCall) {
        return decision;
    }
}
