package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.agent.AgentDefinition;
import io.github.pi_java.agent.domain.agent.InteractionMode;
import io.github.pi_java.agent.domain.agent.RuntimeLimits;
import io.github.pi_java.agent.domain.common.PlatformIds.AgentId;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelRequest;
import io.github.pi_java.agent.domain.model.ModelResponse;
import io.github.pi_java.agent.domain.runtime.CancellationToken;
import io.github.pi_java.agent.domain.runtime.RunContext;
import io.github.pi_java.agent.domain.runtime.RunInput;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.session.SessionContext;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FakeModelContextCaptureTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CURRENT_PROMPT = "Please continue the deployment checklist.";

    @Test
    void non_streaming_fake_captures_prior_turns_before_current_prompt_once() {
        FakeModelClient model = new FakeModelClient()
                .script(new ModelResponse.FinalText("fake-provider", "fake-model", "fake-provider:fake-model",
                        "Done", ModelFinishReason.STOP, null, Duration.ZERO));
        GeneralAgentLoop loop = new GeneralAgentLoop(model, new FakeToolInvoker(), FakePolicy.allow(), new EventCollector(),
                new DeterministicIds(), new DeterministicClock(START));

        assertThat(loop.start(context()).status()).isEqualTo(RunStatus.SUCCEEDED);

        ModelRequest captured = model.lastRequest().orElseThrow();
        assertPriorTurnsBeforeCurrentPrompt(captured);
        assertThat(model.requests()).containsExactly(captured);
    }

    @Test
    void streaming_fake_captures_prior_turns_before_current_prompt_once() {
        FakeStreamingModelClient model = new FakeStreamingModelClient()
                .text("Done")
                .finish(ModelFinishReason.STOP);
        GeneralAgentLoop loop = new GeneralAgentLoop(model, new FakeToolInvoker(), FakePolicy.allow(), new EventCollector(),
                new DeterministicIds(), new DeterministicClock(START));

        assertThat(loop.start(context()).status()).isEqualTo(RunStatus.SUCCEEDED);

        ModelRequest captured = model.lastRequest().orElseThrow();
        assertPriorTurnsBeforeCurrentPrompt(captured);
        assertThat(model.requests()).containsExactly(captured);
    }

    private static void assertPriorTurnsBeforeCurrentPrompt(ModelRequest captured) {
        assertThat(captured.context().sessionContext().messages())
                .extracting(SessionEntryPayload.MessageEntry::role)
                .containsExactly("user", "assistant");
        assertThat(captured.context().sessionContext().messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("What did we decide about provider boundaries?",
                        "Keep provider SDK types outside Domain and App contracts.");
        assertThat(captured.context().input()).isInstanceOfSatisfying(RunInput.ChatInput.class,
                input -> assertThat(input.text()).isEqualTo(CURRENT_PROMPT));

        List<String> modelSemanticInputs = Stream.concat(
                captured.context().sessionContext().messages().stream().map(SessionEntryPayload.MessageEntry::content),
                Stream.of(((RunInput.ChatInput) captured.context().input()).text()))
                .toList();
        assertThat(modelSemanticInputs).containsExactly(
                "What did we decide about provider boundaries?",
                "Keep provider SDK types outside Domain and App contracts.",
                CURRENT_PROMPT);
        assertThat(modelSemanticInputs).filteredOn(CURRENT_PROMPT::equals).hasSize(1);
    }

    private static RunContext context() {
        RuntimeLimits limits = new RuntimeLimits(Duration.ofMinutes(1), 4, 2);
        return new RunContext(
                new AgentDefinition(new AgentId("agent-1"), "General Agent", "Use prior context.", "fake-provider:fake-model",
                        Set.of("workspace"), Set.of("default"), limits, Set.of(InteractionMode.CHAT),
                        "workspace-policy", "output-policy"),
                new RunInput.ChatInput(CURRENT_PROMPT),
                new SessionContext(List.of(
                        new SessionEntryPayload.MessageEntry("user", "What did we decide about provider boundaries?"),
                        new SessionEntryPayload.MessageEntry("assistant", "Keep provider SDK types outside Domain and App contracts.")),
                        List.of(), List.of(), List.of(), List.of(), Optional.empty(), List.of()),
                new WorkspaceScope("tenant-1", "user-1", "session-1", "run-1", "workspace-1", Set.of(), Set.of()),
                limits,
                new CancellationToken(),
                "00000000000000000000000000000001",
                START);
    }
}
