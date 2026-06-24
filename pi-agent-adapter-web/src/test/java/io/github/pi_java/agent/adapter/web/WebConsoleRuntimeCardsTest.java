package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.adapter.web.ui.console.RuntimeEventCard;
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

    private static RunEventDto event(String type, Map<String, Object> payload) {
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
                type,
                1,
                payload);
    }
}
