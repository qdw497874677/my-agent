package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import java.util.ArrayList;
import java.util.List;

/** Secondary Console panel for browsing and switching Agents from the public /api/agents catalog. */
public class AgentCatalogPanel extends Div {

    private final ConsoleHttpClient httpClient;
    private final Div cards = new Div();
    private final List<AgentCard> renderedCards = new ArrayList<>();

    public AgentCatalogPanel() {
        this(new ConsoleHttpClient());
    }

    public AgentCatalogPanel(ConsoleHttpClient httpClient) {
        this.httpClient = httpClient;
        addClassName("pi-agent-catalog");
        getElement().setAttribute("data-secondary", "catalog-switcher");
        cards.getElement().setAttribute("data-role", "agent-catalog-cards");
        add(new H2("Agent Catalog"), new Paragraph("Choose or switch the Agent for the next run."), cards);
        showCatalog(new AgentCatalogResponse(List.of()));
    }

    public void showCatalog(AgentCatalogResponse response) {
        cards.removeAll();
        renderedCards.clear();
        List<AgentCatalogItemDto> agents = response == null || response.agents() == null ? List.of() : response.agents();
        if (agents.isEmpty()) {
            Paragraph empty = new Paragraph("No Agents are available from /api/agents yet.");
            empty.getElement().setAttribute("data-state", "empty-agent-catalog");
            cards.add(empty);
            return;
        }
        for (AgentCatalogItemDto agent : agents) {
            AgentCard card = new AgentCard(agent);
            renderedCards.add(card);
            cards.add(card);
        }
    }

    public String catalogPath() {
        return httpClient.agentCatalogPath();
    }

    public int cardCount() {
        return renderedCards.size();
    }

    public List<String> renderedAgentIds() {
        return renderedCards.stream().map(AgentCard::agentId).toList();
    }

    public String renderedText() {
        return renderedCards.stream().map(AgentCard::summaryText).reduce("", (left, right) -> left + "\n" + right);
    }
}
