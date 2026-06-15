package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Agent Catalog card that exposes run-decision metadata from the public Agent Catalog API. */
public class AgentCard extends Div {

    private final AgentCatalogItemDto agent;
    private final String summaryText;

    public AgentCard(AgentCatalogItemDto agent) {
        this.agent = Objects.requireNonNull(agent, "agent must not be null");
        this.summaryText = buildSummary(agent);
        addClassName("pi-agent-card");
        getElement().setAttribute("data-agent-id", safe(agent.id()));
        getElement().setAttribute("data-action", "choose-agent");
        add(
                new H3(safe(agent.name())),
                new Paragraph(safe(agent.description())),
                line("Input modes", join(agent.supportedInputModes())),
                line("Capabilities", join(agent.capabilities())),
                line("Model", modelRef(agent.modelRef())),
                line("Allowed tools", join(agent.allowedToolIds())),
                line("Scopes", join(agent.allowedToolScopes())),
                line("Risk", join(agent.riskLabels())),
                line("Side effects", join(agent.sideEffectLabels())));
        for (AgentCatalogItemDto.EntryActionDto action : agent.entryActions()) {
            Button button = new Button(safe(action.label()));
            button.getElement().setAttribute("data-entry-action", safe(action.id()));
            button.getElement().setAttribute("data-input-mode", safe(action.inputMode()));
            add(button);
        }
    }

    public String agentId() {
        return agent.id();
    }

    public String summaryText() {
        return summaryText;
    }

    private static Span line(String label, String value) {
        Span span = new Span(label + ": " + value);
        span.getElement().setAttribute("data-field", label.toLowerCase().replace(' ', '-'));
        return span;
    }

    private static String buildSummary(AgentCatalogItemDto agent) {
        return String.join(" | ", List.of(
                safe(agent.name()),
                safe(agent.description()),
                "input=" + join(agent.supportedInputModes()),
                "capabilities=" + join(agent.capabilities()),
                "model=" + modelRef(agent.modelRef()),
                "tools=" + join(agent.allowedToolIds()),
                "scopes=" + join(agent.allowedToolScopes()),
                "risk=" + join(agent.riskLabels()),
                "sideEffects=" + join(agent.sideEffectLabels()),
                "actions=" + agent.entryActions().stream()
                        .map(AgentCatalogItemDto.EntryActionDto::label)
                        .map(AgentCard::safe)
                        .collect(Collectors.joining(", "))));
    }

    private static String modelRef(AgentCatalogItemDto.ModelRefDto modelRef) {
        if (modelRef == null) {
            return "not disclosed";
        }
        if (modelRef.safeRef() != null && !modelRef.safeRef().isBlank()) {
            return modelRef.safeRef();
        }
        return safe(modelRef.provider()) + ":" + safe(modelRef.model());
    }

    private static String join(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }
        return values.stream().map(AgentCard::safe).sorted().collect(Collectors.joining(", "));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
