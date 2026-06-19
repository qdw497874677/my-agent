package io.github.pi_java.agent.infrastructure.observability;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryStreamingModelClientTest {
    private static final String SECRET = "PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK";

    @Test
    void records_model_counter_and_timer_with_safe_provider_model_status_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TelemetryStreamingModelClient client = new TelemetryStreamingModelClient(
                (request, cancellationToken, sink) -> { }, new PiTelemetry(registry, null));

        client.stream(request("openai-compatible:gpt-4.1-mini"), new CancellationToken(), chunk -> { });

        assertThat(registry.get(PiTelemetryNames.MODEL_CALLS_TOTAL)
                .tags("provider", "openai-compatible", "model", "gpt-4.1-mini", "status", "success")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(PiTelemetryNames.MODEL_CALL_DURATION)
                .tags("provider", "openai-compatible", "model", "gpt-4.1-mini", "status", "success")
                .timer().count()).isEqualTo(1L);
    }

    @Test
    void records_errors_without_leaking_provider_body_or_api_key_markers_into_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        StreamingModelClient delegate = (request, cancellationToken, sink) -> {
            throw new IllegalStateException("remote body Authorization Bearer " + SECRET);
        };
        TelemetryStreamingModelClient client = new TelemetryStreamingModelClient(delegate, new PiTelemetry(registry, null));

        assertThatThrownBy(() -> client.stream(request("provider-a:model-a"), new CancellationToken(), chunk -> { }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registry.get(PiTelemetryNames.MODEL_CALLS_TOTAL)
                .tags("provider", "provider-a", "model", "model-a", "status", "error")
                .counter().count()).isEqualTo(1.0d);
        assertThat(allMeterTagValues(registry)).doesNotContain(SECRET, "Authorization", "Bearer");
    }

    private static String allMeterTagValues(SimpleMeterRegistry registry) {
        StringBuilder values = new StringBuilder();
        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> values.append(tag.getValue()).append('\n'));
        }
        return values.toString();
    }

    private static ModelRequest request(String modelRef) {
        RuntimeLimits limits = new RuntimeLimits(Duration.ofSeconds(30), 8, 4);
        AgentDefinition agent = new AgentDefinition(new AgentId("agent-a"), "Agent", "instructions", modelRef,
                Set.of(), Set.of(), limits, Set.of(InteractionMode.CHAT), "workspace-policy", "output-policy");
        WorkspaceScope workspace = new WorkspaceScope("tenant-a", "user-a", "session-a", "run-a", "workspace-a",
                Set.of(), Set.of());
        RunContext context = new RunContext(agent, new RunInput.ChatInput("hello"),
                new SessionContext(List.of(), List.of(), List.of(), List.of(), List.of(), Optional.of(workspace), List.of()),
                workspace, limits, new CancellationToken(), "4bf92f3577b34da6a3ce929d0e0e4736", Instant.now());
        return new ModelRequest(context, List.of());
    }
}
