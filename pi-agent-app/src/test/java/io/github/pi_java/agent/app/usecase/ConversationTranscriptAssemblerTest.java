package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.domain.common.PlatformIds.*;
import io.github.pi_java.agent.domain.error.FailureSummary;
import io.github.pi_java.agent.domain.error.PiError;
import io.github.pi_java.agent.domain.event.EventVisibility;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden behavior coverage for {@link ConversationTranscriptAssembler}
 * implementing decisions D-05, D-06, D-07, D-08, D-13, and D-16. Uses pure
 * in-memory fixtures (no Infrastructure/JDBC/Spring/Vaadin/provider types).
 */
class ConversationTranscriptAssemblerTest {

    private static final String TENANT = "tenant-1";
    private static final String USER = "user-1";
    private static final String SESSION = "session-1";
    private static final String WORKSPACE = "workspace-1";
    private static final String TRACE = "0123456789abcdef0123456789abcdef";
    private static final Instant T0 = Instant.parse("2026-06-28T05:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-28T05:00:01Z");
    private static final Instant T2 = Instant.parse("2026-06-28T05:00:02Z");
    private static final Instant T3 = Instant.parse("2026-06-28T05:00:03Z");

    private final ConversationTranscriptAssembler assembler = new ConversationTranscriptAssembler();

    @Test
    void chatInputAndModelDeltasProduceUserAndCompletedAssistantMessage() {
        ConversationRunView run = run("run-1", T0, Map.of("text", "hello"), "SUCCEEDED");
        List<RunEvent> events = List.of(
                delta("run-1", 1, T1, "Hel"),
                delta("run-1", 2, T2, "lo there"),
                runLifecycle("run-1", 3, T3, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED, null));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(run), events);

        assertThat(messages).extracting(ConversationMessageDto::role)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);

        ConversationMessageDto user = messages.get(0);
        assertThat(user.text()).isEqualTo("hello");
        assertThat(user.status()).isEqualTo(ConversationMessageStatus.COMPLETED);
        assertThat(user.runId()).isEqualTo("run-1");
        assertThat(user.sessionId()).isEqualTo(SESSION);

        ConversationMessageDto assistant = messages.get(1);
        assertThat(assistant.text()).isEqualTo("Hello there");
        assertThat(assistant.status()).isEqualTo(ConversationMessageStatus.COMPLETED);
        assertThat(assistant.firstSequence()).isEqualTo(1L);
        assertThat(assistant.lastSequence()).isEqualTo(2L);
        assertThat(assistant.createdAt()).isEqualTo(T1);
        assertThat(assistant.updatedAt()).isEqualTo(T2);
    }

    @Test
    void failedRunWithNoDeltasProducesErrorWithoutBlankAssistant() {
        ConversationRunView run = run("run-1", T0, Map.of("text", "hello"), "FAILED");
        FailureSummary failure = new FailureSummary("provider timeout",
                new PiError(PiError.Category.MODEL, "MODEL_TIMEOUT", PiError.Severity.ERROR, EventVisibility.USER, false, false, false));
        List<RunEvent> events = List.of(
                runLifecycle("run-1", 1, T1, RunEventType.RUN_FAILED, RunStatus.FAILED, failure));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(run), events);

        assertThat(messages).extracting(ConversationMessageDto::role)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ERROR);
        ConversationMessageDto error = messages.get(1);
        assertThat(error.status()).isEqualTo(ConversationMessageStatus.FAILED);
        assertThat(error.text()).contains("provider timeout");
        // finish/error events must not create blank assistant text
        assertThat(messages).filteredOn(m -> m.role() == ConversationMessageRole.ASSISTANT).isEmpty();
    }

    @Test
    void failedRunWithPriorDeltasProducesAssistantFailedState() {
        ConversationRunView run = run("run-1", T0, Map.of("text", "hello"), "FAILED");
        FailureSummary failure = new FailureSummary("provider timeout",
                new PiError(PiError.Category.MODEL, "MODEL_TIMEOUT", PiError.Severity.ERROR, EventVisibility.USER, false, false, false));
        List<RunEvent> events = List.of(
                delta("run-1", 1, T1, "partial "),
                delta("run-1", 2, T2, "reply"),
                runLifecycle("run-1", 3, T3, RunEventType.RUN_FAILED, RunStatus.FAILED, failure));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(run), events);

        ConversationMessageDto assistant = messages.stream()
                .filter(m -> m.role() == ConversationMessageRole.ASSISTANT)
                .findFirst().orElseThrow();
        assertThat(assistant.text()).isEqualTo("partial reply");
        assertThat(assistant.status()).isEqualTo(ConversationMessageStatus.FAILED);
    }

    @Test
    void cancelledRunWithPriorDeltasProducesCancelledAssistantPreservingTextAndSequenceRange() {
        ConversationRunView run = run("run-1", T0, Map.of("text", "hello"), "CANCELLED");
        List<RunEvent> events = List.of(
                delta("run-1", 1, T1, "partial "),
                delta("run-1", 2, T2, "reply"),
                runLifecycle("run-1", 3, T3, RunEventType.RUN_CANCELLED, RunStatus.CANCELLED, null));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(run), events);

        ConversationMessageDto assistant = messages.stream()
                .filter(m -> m.role() == ConversationMessageRole.ASSISTANT)
                .findFirst().orElseThrow();
        assertThat(assistant.text()).isEqualTo("partial reply");
        assertThat(assistant.status()).isIn(ConversationMessageStatus.CANCELLED, ConversationMessageStatus.PARTIAL);
        assertThat(assistant.firstSequence()).isEqualTo(1L);
        assertThat(assistant.lastSequence()).isEqualTo(2L);
    }

    @Test
    void toolLifecycleEventProducesRedactedToolMessageWithoutRawSecrets() {
        ConversationRunView run = run("run-1", T0, Map.of("text", "run the tool"), "SUCCEEDED");

        Map<String, Object> redactedInput = new LinkedHashMap<>();
        redactedInput.put("path", "/repo/file.txt");
        redactedInput.put("executor", "ShellExecutor");   // sensitive -> must be stripped
        redactedInput.put("class", "com.acme.Shell");      // sensitive -> must be stripped
        redactedInput.put("api_key", "sk-very-secret");    // sensitive -> must be stripped

        RunEventPayload.ToolLifecyclePayload toolPayload = new RunEventPayload.ToolLifecyclePayload(
                "tool-call-1", "fs.read", "v1",
                new ToolProvenance(ToolProvenance.SourceKind.BUILT_IN, "builtin", "fs.read", Map.of()),
                redactedInput,
                Map.of("bytes", 42),
                PolicyDecision.ALLOW,
                ToolExecutionStatus.SUCCESS,
                null,
                null);

        List<RunEvent> events = List.of(
                delta("run-1", 1, T1, "model "),
                tool("run-1", 2, T2, toolPayload),
                delta("run-1", 3, T2, "reply"),
                runLifecycle("run-1", 4, T3, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED, null));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(run), events);

        ConversationMessageDto toolMessage = messages.stream()
                .filter(m -> m.role() == ConversationMessageRole.TOOL)
                .findFirst().orElseThrow();
        assertThat(toolMessage.runId()).isEqualTo("run-1");
        assertThat(toolMessage.firstSequence()).isEqualTo(2L);
        assertThat(toolMessage.lastSequence()).isEqualTo(2L);
        assertThat(toolMessage.redacted()).isTrue();

        Map<String, Object> metadata = toolMessage.metadata();
        assertThat(metadata).containsKeys("toolId", "toolCallId", "executionStatus", "inputSummary", "outputSummary");
        assertThat(metadata.get("toolId")).isEqualTo("fs.read");
        assertThat(metadata).doesNotContainKey("executor").doesNotContainKey("class")
                .doesNotContainKey("api_key").doesNotContainKey("apikey").doesNotContainKey("secret").doesNotContainKey("token");

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSummary = (Map<String, Object>) metadata.get("inputSummary");
        assertThat(inputSummary).containsEntry("path", "/repo/file.txt");
        assertThat(inputSummary).doesNotContainKey("executor").doesNotContainKey("class")
                .doesNotContainKey("api_key").doesNotContainKey("apikey");
    }

    @Test
    void messagesOrderedByRunCreationThenEventSequenceAndCarryRefs() {
        ConversationRunView earlier = run("run-1", T0, Map.of("text", "first"), "SUCCEEDED");
        ConversationRunView later = run("run-2", T3, Map.of("text", "second"), "SUCCEEDED");

        List<RunEvent> events = List.of(
                delta("run-1", 1, T1, "first reply"),
                runLifecycle("run-1", 2, T2, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED, null),
                delta("run-2", 1, T3, "second reply"),
                runLifecycle("run-2", 2, T3, RunEventType.RUN_COMPLETED, RunStatus.SUCCEEDED, null));

        List<ConversationMessageDto> messages = assembler.assemble(SESSION, List.of(later, earlier), events);

        // Run-1 messages (by createdAt ordering) come before run-2 despite run-2 being supplied first.
        assertThat(messages).extracting(ConversationMessageDto::runId)
                .containsExactly("run-1", "run-1", "run-2", "run-2");
        assertThat(messages).extracting(ConversationMessageDto::role)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT,
                        ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);
        // Every message carries sessionId and a non-null firstSequence/lastSequence for event-derived items.
        assertThat(messages).allSatisfy(m -> assertThat(m.sessionId()).isEqualTo(SESSION));
        ConversationMessageDto assistant = messages.get(1);
        assertThat(assistant.firstSequence()).isEqualTo(1L);
        assertThat(assistant.lastSequence()).isEqualTo(1L);
    }

    // ---- fixtures ----

    private static ConversationRunView run(String runId, Instant createdAt, Map<String, Object> input, String status) {
        return new ConversationRunView(runId, createdAt, input, status);
    }

    private static RunEvent delta(String runId, long sequence, Instant timestamp, String textDelta) {
        return event(runId, sequence, timestamp, RunEventType.MODEL_DELTA, "step-" + sequence,
                new RunEventPayload.ModelDeltaPayload("model-ref-1", textDelta), EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }

    private static RunEvent tool(String runId, long sequence, Instant timestamp, RunEventPayload.ToolLifecyclePayload payload) {
        return event(runId, sequence, timestamp, RunEventType.TOOL_COMPLETED, "step-" + sequence,
                payload, EventVisibility.USER, new RedactionMetadata(false, true, Set.of(), "tool-redaction"));
    }

    private static RunEvent runLifecycle(String runId, long sequence, Instant timestamp, RunEventType type, RunStatus status,
                                         FailureSummary failure) {
        return event(runId, sequence, timestamp, type, "step-none",
                new RunEventPayload.RunLifecyclePayload(status, failure), EventVisibility.USER,
                new RedactionMetadata(false, false, Set.of(), "default"));
    }

    private static RunEvent event(String runId, long sequence, Instant timestamp, RunEventType type, String stepId,
                                  RunEventPayload payload, EventVisibility visibility, RedactionMetadata redaction) {
        return new RunEvent(
                "event-" + runId + "-" + sequence,
                new TenantId(TENANT), new UserId(USER), new SessionId(SESSION),
                new RunId(runId), new StepId(stepId), new WorkspaceId(WORKSPACE),
                sequence, timestamp, type, new TraceId(TRACE), new CorrelationId("corr-1"), new CausationId("cause-1"),
                payload, visibility, redaction);
    }
}
