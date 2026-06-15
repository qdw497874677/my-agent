package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.console.AgentCatalogPanel;
import io.github.pi_java.agent.adapter.web.ui.console.AgentCard;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebConsoleCatalogAndToolCardsTest {

    @Test
    void catalogCardsRenderRunDecisionMetadataAndEntryActions() {
        AgentCatalogItemDto agent = catalogAgent("cloud-general-agent", "Cloud General Agent");

        AgentCard card = new AgentCard(agent);

        assertThat(card.agentId()).isEqualTo("cloud-general-agent");
        assertThat(card.summaryText())
                .contains("Cloud General Agent")
                .contains("General purpose agent")
                .contains("chat")
                .contains("tool-use")
                .contains("openai:gpt-4.1-mini")
                .contains("builtin.info")
                .contains("workspace:read")
                .contains("LOW")
                .contains("READ_ONLY")
                .contains("Start chat");
        assertThat(card.getElement().getAttribute("data-agent-id")).isEqualTo("cloud-general-agent");
        assertThat(card.getElement().getAttribute("data-action")).isEqualTo("choose-agent");
    }

    @Test
    void catalogPanelUsesApiResponseFixturesNotHardcodedAgents() {
        AgentCatalogResponse response = new AgentCatalogResponse(List.of(
                catalogAgent("agent-a", "Agent A"),
                catalogAgent("agent-b", "Agent B")));

        AgentCatalogPanel panel = new AgentCatalogPanel();
        panel.showCatalog(response);

        assertThat(panel.catalogPath()).isEqualTo("/api/agents");
        assertThat(panel.cardCount()).isEqualTo(2);
        assertThat(panel.renderedAgentIds()).containsExactly("agent-a", "agent-b");
        assertThat(panel.renderedText()).contains("Agent A").contains("Agent B");
    }

    @Test
    void consoleKeepsCatalogAvailableWithoutMakingItDominantLandingExperience() {
        ConsoleView view = new ConsoleView();

        assertThat(view.chatPanel().getElement().getAttribute("data-primary")).isEqualTo("chat-first");
        assertThat(view.agentCatalogPanel().getElement().getAttribute("data-secondary")).isEqualTo("catalog-switcher");
        assertThat(view.agentCatalogPlan().path()).isEqualTo("/api/agents");
    }

    private static AgentCatalogItemDto catalogAgent(String id, String name) {
        return new AgentCatalogItemDto(
                id,
                name,
                "General purpose agent for cloud runtime work",
                Set.of("chat", "task"),
                Set.of("tool-use", "streaming"),
                new AgentCatalogItemDto.ModelRefDto("openai", "gpt-4.1-mini", "openai:gpt-4.1-mini"),
                Set.of("builtin.info", "builtin.workspace.write"),
                Set.of("workspace:read", "workspace:write"),
                Set.of("LOW", "MEDIUM"),
                Set.of("READ_ONLY", "SIDE_EFFECTFUL"),
                List.of(new AgentCatalogItemDto.EntryActionDto(
                        "start-chat", "Start chat", "chat", "chat", Map.of("focus", "input"))),
                Duration.ofMinutes(5),
                Map.of("source", "fixture"));
    }
}
