package io.github.pi_java.agent.domain.agent;

import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.runtime.RunInput;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AgentDefinitionTest {

    @Test
    void agent_definition_exposes_immutable_capability_declaration_without_framework_types() {
        Set<String> toolScopes = new java.util.HashSet<>(Set.of("workspace.read", "tool.safe"));
        Set<String> policyRefs = new java.util.HashSet<>(Set.of("policy.default", "policy.redaction"));
        Set<InteractionMode> modes = EnumSet.allOf(InteractionMode.class);

        AgentDefinition definition = new AgentDefinition(
                new AgentId("agent-general"),
                "General Agent",
                "Follow platform policy and solve the user objective.",
                "openai-compatible:gpt-4.1-mini",
                toolScopes,
                policyRefs,
                new RuntimeLimits(Duration.ofMinutes(5), 12, 8),
                modes,
                "workspace.default",
                "output.redacted"
        );

        toolScopes.add("mutated.scope");
        policyRefs.add("mutated.policy");
        modes.remove(InteractionMode.WORKFLOW_PLANNER);

        assertThat(definition.agentId()).isEqualTo(new AgentId("agent-general"));
        assertThat(definition.displayName()).isEqualTo("General Agent");
        assertThat(definition.instructions()).contains("platform policy");
        assertThat(definition.modelRef()).isEqualTo("openai-compatible:gpt-4.1-mini");
        assertThat(definition.allowedToolScopes()).containsExactlyInAnyOrder("workspace.read", "tool.safe");
        assertThat(definition.policyRefs()).containsExactlyInAnyOrder("policy.default", "policy.redaction");
        assertThat(definition.runtimeLimits().maxSteps()).isEqualTo(12);
        assertThat(definition.supportedInputModes()).contains(InteractionMode.WORKFLOW_PLANNER);
        assertThat(definition.supportedInputModes()).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(InteractionMode.class));
        assertThat(definition.workspacePolicyRef()).isEqualTo("workspace.default");
        assertThat(definition.outputPolicyRef()).isEqualTo("output.redacted");

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> definition.allowedToolScopes().add("another.scope"));
    }

    @Test
    void run_input_has_distinct_non_chat_only_variants() {
        RunInput chat = new RunInput.ChatInput("hello");
        RunInput task = new RunInput.TaskInput("summarize runtime goals");
        RunInput structuredForm = new RunInput.StructuredFormInput(Map.of("priority", "high"));
        RunInput toolDriven = new RunInput.ToolDrivenInput("lookupCustomer", Map.of("id", 42));
        RunInput planner = new RunInput.WorkflowPlannerInput("plan a governed workflow");

        assertThat(chat).isInstanceOf(RunInput.ChatInput.class);
        assertThat(task).isInstanceOf(RunInput.TaskInput.class);
        assertThat(structuredForm).isInstanceOf(RunInput.StructuredFormInput.class);
        assertThat(toolDriven).isInstanceOf(RunInput.ToolDrivenInput.class);
        assertThat(planner).isInstanceOf(RunInput.WorkflowPlannerInput.class);
    }
}
