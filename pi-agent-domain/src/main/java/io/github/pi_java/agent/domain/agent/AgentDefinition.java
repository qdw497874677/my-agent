package io.github.pi_java.agent.domain.agent;

import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;

import java.util.Objects;
import java.util.Set;

public record AgentDefinition(
        AgentId agentId,
        String displayName,
        String instructions,
        String modelRef,
        Set<String> allowedToolScopes,
        Set<String> policyRefs,
        RuntimeLimits runtimeLimits,
        Set<InteractionMode> supportedInputModes,
        String workspacePolicyRef,
        String outputPolicyRef
) {

    public AgentDefinition {
        Objects.requireNonNull(agentId, "agentId must not be null");
        displayName = requireNonBlank(displayName, "displayName");
        instructions = requireNonBlank(instructions, "instructions");
        modelRef = requireNonBlank(modelRef, "modelRef");
        allowedToolScopes = Set.copyOf(Objects.requireNonNull(allowedToolScopes, "allowedToolScopes must not be null"));
        policyRefs = Set.copyOf(Objects.requireNonNull(policyRefs, "policyRefs must not be null"));
        runtimeLimits = Objects.requireNonNull(runtimeLimits, "runtimeLimits must not be null");
        supportedInputModes = Set.copyOf(Objects.requireNonNull(supportedInputModes, "supportedInputModes must not be null"));
        workspacePolicyRef = requireNonBlank(workspacePolicyRef, "workspacePolicyRef");
        outputPolicyRef = requireNonBlank(outputPolicyRef, "outputPolicyRef");
        if (supportedInputModes.isEmpty()) {
            throw new IllegalArgumentException("supportedInputModes must not be empty");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
