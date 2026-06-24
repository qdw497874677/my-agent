package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.console.ApprovalCard;
import io.github.pi_java.agent.adapter.web.ui.console.RuntimeEventCard;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.adapter.web.ui.console.ToolCallCard;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebConsoleRuntimeCardsTest {

    @Test
    void runtimeCardVisibleSummaryIncludesStatusTypeTimestampAndShortSummary() {
        RunEventDto event = event("run.status", Map.of(
                "status", "RUNNING",
                "message", "Runtime is preparing the model call and event stream."));

        RuntimeEventCard card = RuntimeEventCard.from(event, "status", "RUNNING", "Preparing runtime stream");

        assertThat(card.summaryText())
                .contains("RUNNING")
                .contains("run.status")
                .contains("2026-06-15T05:00:00Z")
                .contains("Preparing runtime stream");
    }

    @Test
    void runtimeCardDetailsRedactSensitiveMarkersAndBoundLongDiagnostics() {
        String longUrl = "https://example.test/" + "x".repeat(180);
        RunEventDto event = event("run.status", Map.of(
                "api_key", "api_key=sk-live-secret",
                "password", "password=hunter2",
                "token", "token=raw-token-value",
                "secret", "secret=my-secret",
                "url", longUrl));

        RuntimeEventCard card = RuntimeEventCard.from(event, "status", "RUNNING", "Sensitive diagnostics");

        assertThat(card.detailsText())
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value")
                .doesNotContain("hunter2")
                .doesNotContain("my-secret");
        assertThat(card.advancedDetailText())
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value")
                .doesNotContain("hunter2")
                .doesNotContain("my-secret")
                .doesNotContain(longUrl);
    }

    @Test
    void runtimeCardExposesStableTimelineAttributesAndLayeredDetailMarkers() {
        RuntimeEventCard card = RuntimeEventCard.from(event("run.status", Map.of("status", "RUNNING")),
                "status", "RUNNING", "Runtime running");

        assertThat(card.getElement().getAttribute("data-event-category")).isEqualTo("status");
        assertThat(card.getElement().getAttribute("data-event-type")).isEqualTo("run.status");
        assertThat(card.getElement().getAttribute("data-event-status")).isEqualTo("RUNNING");
        assertThat(card.getElement().getAttribute("data-expandable")).isEqualTo("true");
        assertThat(card.getElement().getAttribute("data-layered-detail")).isEqualTo("true");
    }

    @Test
    void rendererKeepsModelDeltaTextAndReturnsRuntimeEventCard() {
        RunEventRenderer renderer = new RunEventRenderer();

        RunEventRenderer.RenderedEvent rendered = renderer.render(event("model.delta", Map.of("text", "streamed model content")));

        assertThat(rendered.category()).isEqualTo("model");
        assertThat(rendered.terminal()).isFalse();
        assertThat(rendered.text()).contains("streamed model content");
        assertThat(rendered.component()).isInstanceOf(RuntimeEventCard.class);
    }

    @Test
    void rendererReturnsRuntimeCardsForPolicyStatusTerminalAndGenericEvents() {
        RunEventRenderer renderer = new RunEventRenderer();

        RunEventRenderer.RenderedEvent policy = renderer.render(event("policy.decision", Map.of("decision", "ALLOW")));
        RunEventRenderer.RenderedEvent status = renderer.render(event("run.status", Map.of("status", "RUNNING")));
        RunEventRenderer.RenderedEvent terminal = renderer.render(event("run.completed", Map.of("status", "COMPLETED")));
        RunEventRenderer.RenderedEvent generic = renderer.render(event("runtime.heartbeat", Map.of("message", "still alive")));

        assertThat(policy.category()).isEqualTo("policy");
        assertThat(policy.text()).contains("Policy:");
        assertThat(policy.component()).isInstanceOf(RuntimeEventCard.class);
        assertThat(status.category()).isEqualTo("status");
        assertThat(status.text()).contains("Run status:");
        assertThat(status.component()).isInstanceOf(RuntimeEventCard.class);
        assertThat(terminal.category()).isEqualTo("terminal");
        assertThat(terminal.text()).contains("Run terminal:");
        assertThat(terminal.component()).isInstanceOf(RuntimeEventCard.class);
        assertThat(generic.category()).isEqualTo("event");
        assertThat(generic.component()).isInstanceOf(RuntimeEventCard.class);
    }

    @Test
    void rendererKeepsToolAndApprovalEventsOnSpecializedCards() {
        RunEventRenderer renderer = new RunEventRenderer(new ConsoleHttpClient());

        RunEventRenderer.RenderedEvent tool = renderer.render(event("tool.started", Map.of(
                "toolName", "builtin.info",
                "status", "STARTED"), "tool.lifecycle"));
        RunEventRenderer.RenderedEvent approval = renderer.render(event("tool.approval_required", Map.of(
                "toolCallId", "tool-call-1",
                "toolId", "builtin.workspace.write",
                "toolName", "builtin.workspace.write",
                "status", "APPROVAL_REQUIRED",
                "previewId", "preview-1"), "tool.lifecycle"));

        assertThat(tool.category()).isEqualTo("tool");
        assertThat(tool.component()).isInstanceOf(ToolCallCard.class);
        assertThat(tool.component()).isNotInstanceOf(RuntimeEventCard.class);
        assertThat(approval.category()).isEqualTo("approval");
        assertThat(approval.component()).isInstanceOf(ApprovalCard.class);
        assertThat(approval.component()).isNotInstanceOf(RuntimeEventCard.class);
    }

    @Test
    void toolCardUsesLayeredDetailsForStructuredAndAdvancedRedactedData() {
        RunEventDto event = event("tool.completed", Map.ofEntries(
                Map.entry("toolName", "builtin.workspace.write"),
                Map.entry("status", "COMPLETED"),
                Map.entry("inputSummary", "Write report with password=hunter2"),
                Map.entry("argumentSummary", "authorization=Bearer abc123"),
                Map.entry("outputSummary", "Created artifact with raw-token-value"),
                Map.entry("resultSummary", "Stored result for sk-live-secret"),
                Map.entry("preview", Map.of("command", "write", "api_key", "api_key=sk-live-secret")),
                Map.entry("diagnostics", Map.of("token", "token=raw-token-value")),
                Map.entry("eventSequence", 42),
                Map.entry("policyReason", "write requires policy check"),
                Map.entry("previewId", "preview-42")), "tool.lifecycle");

        ToolCallCard card = ToolCallCard.from(event);

        assertThat(card.detailsText())
                .contains("sequence=7")
                .contains("type=tool.lifecycle")
                .contains("payloadSchema=tool.completed")
                .contains("eventSequence=42")
                .contains("write requires policy check")
                .contains("preview-42")
                .contains("[REDACTED]")
                .doesNotContain("sk-live-secret")
                .doesNotContain("raw-token-value")
                .doesNotContain("password=hunter2")
                .doesNotContain("authorization=Bearer abc123");
    }

    private static RunEventDto event(String type, Map<String, Object> payload) {
        return event(type, payload, type);
    }

    private static RunEventDto event(String type, Map<String, Object> payload, String payloadSchema) {
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
                payloadSchema,
                "trace",
                "correlation",
                null,
                "USER",
                null,
                type,
                1,
                payload);
    }
}
