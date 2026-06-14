package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolPolicyEvaluator;
import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Conservative default tool policy per Phase 4 D-09.
 */
public final class DefaultToolPolicyEvaluator implements ToolPolicyEvaluator {

    public static final String DEFAULT_POLICY_REF = "default-tool-policy";

    private final AgentDefinition agentDefinition;
    private final Set<String> additionalAllowedScopes;

    public DefaultToolPolicyEvaluator(AgentDefinition agentDefinition) {
        this(agentDefinition, Set.of());
    }

    public DefaultToolPolicyEvaluator(AgentDefinition agentDefinition, Set<String> additionalAllowedScopes) {
        this.agentDefinition = Objects.requireNonNull(agentDefinition, "agentDefinition must not be null");
        this.additionalAllowedScopes = Set.copyOf(Objects.requireNonNull(additionalAllowedScopes, "additionalAllowedScopes must not be null"));
    }

    @Override
    public PolicyEvaluation evaluate(PolicyEvaluationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ToolDescriptor descriptor = request.descriptor();
        Map<String, Object> metadata = metadata(request);
        if (descriptor.riskLevel() == ToolRiskLevel.CRITICAL || descriptor.sideEffect() == ToolSideEffect.DESTRUCTIVE) {
            return new PolicyEvaluation(PolicyDecision.BLOCK, "destructive or critical tools are blocked by default",
                    DEFAULT_POLICY_REF, true, Optional.empty(), Optional.empty(), metadata);
        }
        if (!scopesAllowed(descriptor.scopes())) {
            return new PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL, "tool scope is not pre-authorized for this agent",
                    DEFAULT_POLICY_REF, true, Optional.of("approval:scope:" + descriptor.id()), Optional.empty(), metadata);
        }
        if (descriptor.riskLevel() == ToolRiskLevel.HIGH) {
            return new PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL, "high risk tools require approval by default",
                    DEFAULT_POLICY_REF, true, Optional.of("approval:risk:" + descriptor.id()), Optional.empty(), metadata);
        }
        if (descriptor.sideEffect() == ToolSideEffect.WORKSPACE_WRITE || descriptor.sideEffect() == ToolSideEffect.EXTERNAL_WRITE) {
            return new PolicyEvaluation(PolicyDecision.REQUIRE_APPROVAL, "side-effectful tools require preview and approval by default",
                    DEFAULT_POLICY_REF, true, Optional.of("approval:side-effect:" + descriptor.id()), Optional.empty(), metadata);
        }
        if (descriptor.sideEffect() == ToolSideEffect.EXTERNAL_READ || descriptor.riskLevel() == ToolRiskLevel.MEDIUM) {
            return new PolicyEvaluation(PolicyDecision.ALLOW, "read-only tool allowed with preview metadata",
                    DEFAULT_POLICY_REF, true, Optional.empty(), Optional.empty(), metadata);
        }
        return new PolicyEvaluation(PolicyDecision.ALLOW, "safe read-only tool allowed by default",
                DEFAULT_POLICY_REF, false, Optional.empty(), Optional.empty(), metadata);
    }

    private boolean scopesAllowed(Set<String> descriptorScopes) {
        if (descriptorScopes.isEmpty()) {
            return true;
        }
        java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>(agentDefinition.allowedToolScopes());
        allowed.addAll(additionalAllowedScopes);
        return allowed.containsAll(descriptorScopes);
    }

    private Map<String, Object> metadata(PolicyEvaluationRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenantId", request.context().tenantId());
        metadata.put("userId", request.context().userId());
        metadata.put("workspacePolicyRef", agentDefinition.workspacePolicyRef());
        metadata.put("agentPolicyRefs", agentDefinition.policyRefs());
        metadata.put("descriptorScopes", request.descriptor().scopes());
        metadata.put("allowedToolScopes", agentDefinition.allowedToolScopes());
        metadata.put("riskLevel", request.descriptor().riskLevel().name());
        metadata.put("sideEffect", request.descriptor().sideEffect().name());
        return Map.copyOf(metadata);
    }
}
