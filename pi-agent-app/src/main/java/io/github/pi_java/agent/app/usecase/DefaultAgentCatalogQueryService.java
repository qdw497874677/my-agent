package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DefaultAgentCatalogQueryService implements AgentCatalogQueryService {

    private static final AgentCatalogItemDto DEFAULT_GENERAL_AGENT = new AgentCatalogItemDto(
            "cloud-general-agent",
            "Cloud General Agent",
            "General purpose cloud agent for chat, task execution, and governed tool use.",
            Set.of("CHAT", "TASK"),
            Set.of("chat", "task", "tool-calling", "workspace", "streaming-events"),
            new AgentCatalogItemDto.ModelRefDto("openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini"),
            Set.of("builtin.read_info", "builtin.write_resource", "builtin.workspace_command"),
            Set.of("tool:read", "tool:workspace:write", "tool:workspace:command"),
            Set.of("LOW", "MEDIUM"),
            Set.of("READ_ONLY", "WORKSPACE_WRITE", "COMMAND_EXECUTION"),
            List.of(
                    new AgentCatalogItemDto.EntryActionDto(
                            "start-chat", "Start chat", "CREATE_RUN", "CHAT", Map.of("inputType", "chat")),
                    new AgentCatalogItemDto.EntryActionDto(
                            "start-task", "Run task", "CREATE_RUN", "TASK", Map.of("inputType", "task"))),
            Duration.ofSeconds(30),
            Map.of(
                    "workspacePolicyRef", "default-workspace-policy",
                    "outputPolicyRef", "default-output-policy",
                    "policyRefs", Set.of("default-tool-policy")));

    private final List<AgentCatalogItemDto> agents;

    public DefaultAgentCatalogQueryService() {
        this(List.of(DEFAULT_GENERAL_AGENT));
    }

    public DefaultAgentCatalogQueryService(List<AgentCatalogItemDto> agents) {
        this.agents = List.copyOf(Objects.requireNonNull(agents, "agents must not be null"));
        if (this.agents.isEmpty()) {
            throw new IllegalArgumentException("agents must not be empty");
        }
    }

    @Override
    public AgentCatalogResponse listAgents(RequestContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new AgentCatalogResponse(agents);
    }
}
