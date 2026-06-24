package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.console.AgentCatalogPanel;
import io.github.pi_java.agent.adapter.web.ui.console.AgentCard;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.adapter.web.ui.console.ToolCallCard;
import io.github.pi_java.agent.client.agent.AgentCatalogItemDto;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.time.Duration;
import java.time.Instant;
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

    @Test
    void toolCardSummaryShowsLifecycleFieldsAndRedactedOutcome() {
        RunEventDto completed = toolEvent("tool.completed", Map.ofEntries(
                Map.entry("toolName", "workspace.write"),
                Map.entry("source", "builtin"),
                Map.entry("status", "COMPLETED"),
                Map.entry("policyDecision", "ALLOW"),
                Map.entry("approvalState", "APPROVED"),
                Map.entry("durationMs", 245),
                Map.entry("purpose", "Write generated report"),
                Map.entry("riskLevel", "MEDIUM"),
                Map.entry("sideEffect", "WORKSPACE_WRITE"),
                Map.entry("progress", "100%"),
                Map.entry("resultSummary", "Wrote report.md with token [REDACTED]"),
                Map.entry("errorCategory", "none")));

        ToolCallCard card = ToolCallCard.from(completed);

        assertThat(card.summaryText())
                .contains("Tool", "workspace.write")
                .contains("Source", "builtin")
                .contains("Status", "COMPLETED")
                .contains("Policy", "ALLOW")
                .contains("Approval", "APPROVED")
                .contains("Duration", "245")
                .contains("Error", "none")
                .contains("Summary", "Wrote report.md")
                .contains("Write generated report")
                .contains("MEDIUM")
                .contains("WORKSPACE_WRITE")
                .contains("100%")
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret");
        assertThat(card.detailsText()).contains("workspace.write");
        assertThat(card.getElement().getAttribute("data-event-category")).isEqualTo("tool");
        assertThat(card.getElement().getAttribute("data-tool-status")).isEqualTo("COMPLETED");
        assertThat(card.getElement().getAttribute("data-tool-name")).isEqualTo("workspace.write");
        assertThat(card.getElement().getAttribute("data-tool-source")).isEqualTo("builtin");
        assertThat(card.getElement().getAttribute("data-policy-state")).isEqualTo("ALLOW");
        assertThat(card.getElement().getAttribute("data-expandable")).isEqualTo("true");
    }

    @Test
    void expandedToolDetailsShowSequenceDiagnosticsAndNeverRawSecretValues() {
        RunEventDto approval = toolEvent("tool.approval_required", Map.of(
                "toolName", "workspace.command",
                "status", "REQUIRE_APPROVAL",
                "eventSequence", List.of("tool.proposed", "tool.policy_decided", "tool.preview_generated", "tool.approval_required"),
                "policyReason", "side effect requires approval",
                "previewId", "preview-123",
                "preview", Map.of("impact", "Would run allowlisted command with secret [REDACTED]"),
                "diagnostics", Map.of("credential", "[REDACTED]", "raw", "[REDACTED]"),
                "fakeSecret", "[REDACTED]"));

        ToolCallCard card = ToolCallCard.from(approval);

        assertThat(card.detailsText())
                .contains("tool.proposed")
                .contains("tool.approval_required")
                .contains("side effect requires approval")
                .contains("preview-123")
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value");
    }

    @Test
    void runEventRendererReturnsToolCallCardForToolLifecycleSchema() {
        RunEventRenderer renderer = new RunEventRenderer();

        RunEventRenderer.RenderedEvent rendered = renderer.render(toolEvent("tool.started", Map.of(
                "toolName", "builtin.info",
                "status", "STARTED",
                "summary", "Reading platform metadata")));

        assertThat(rendered.category()).isEqualTo("tool");
        assertThat(rendered.component()).isInstanceOf(ToolCallCard.class);
        assertThat(rendered.text()).contains("builtin.info").contains("STARTED");
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

    private static RunEventDto toolEvent(String type, Map<String, Object> payload) {
        return new RunEventDto(
                "event-1",
                "tenant",
                "user",
                "session-1",
                "run-1",
                null,
                "workspace",
                7,
                Instant.parse("2026-06-15T05:00:00Z"),
                type,
                "trace",
                "correlation",
                null,
                "USER",
                null,
                "tool.lifecycle",
                1,
                payload);
    }
}
