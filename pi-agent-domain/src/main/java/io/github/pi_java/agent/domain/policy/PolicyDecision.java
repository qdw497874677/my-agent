package io.github.pi_java.agent.domain.policy;

public enum PolicyDecision {
    ALLOW,
    DENY,
    REQUIRE_APPROVAL,
    REQUIRE_SANDBOX,
    BLOCK;

    public boolean isTerminalBlock() {
        return this == DENY || this == BLOCK;
    }

    public boolean requiresHumanOrSandboxGate() {
        return this == REQUIRE_APPROVAL || this == REQUIRE_SANDBOX;
    }
}
