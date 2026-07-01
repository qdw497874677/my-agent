package io.github.pi_java.agent.app.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ConversationContextAssemblerTest {

    @Test
    void policyDefaultsAreConservativeAndExposeTranscriptLimit() {
        ConversationContextPolicy policy = ConversationContextPolicy.defaults();

        assertThat(policy.maxRecentMessages()).isEqualTo(12);
        assertThat(policy.maxTotalCharacters()).isEqualTo(12_000);
        assertThat(policy.transcriptLimit()).isGreaterThanOrEqualTo(policy.maxRecentMessages());
    }

    @Test
    void policyRejectsNonPositiveBudgets() {
        assertThatThrownBy(() -> new ConversationContextPolicy(0, 12_000, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRecentMessages");
        assertThatThrownBy(() -> new ConversationContextPolicy(12, 0, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTotalCharacters");
        assertThatThrownBy(() -> new ConversationContextPolicy(12, 12_000, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transcriptLimit");
    }

    @Test
    void metadataRecordsContextCountsAndTruncationState() {
        ConversationContextMetadata metadata = new ConversationContextMetadata(
                3,
                2,
                4,
                12_000,
                450,
                true);

        assertThat(metadata.includedCount()).isEqualTo(3);
        assertThat(metadata.droppedCount()).isEqualTo(2);
        assertThat(metadata.excludedCount()).isEqualTo(4);
        assertThat(metadata.maxTotalCharacters()).isEqualTo(12_000);
        assertThat(metadata.resultingCharacters()).isEqualTo(450);
        assertThat(metadata.truncated()).isTrue();
    }

    @Test
    void assemblerIncludesEligiblePriorUserAndAssistantMessagesInChronologicalOrder() {
        RecordingConversationQueryService queryService = new RecordingConversationQueryService(List.of(
                message("m1", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "Hello"),
                message("m2", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "Hi there"),
                message("m3", "run-2", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "Continue")));
        ConversationContextAssembler assembler = new ConversationContextAssembler(queryService, ConversationContextPolicy.defaults());

        ConversationContextAssembler.Result result = assembler.assemble(requestContext(), "session-1", "run-3");

        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::role)
                .containsExactly("user", "assistant", "user");
        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("Hello", "Hi there", "Continue");
        assertThat(result.metadata().includedCount()).isEqualTo(3);
        assertThat(result.metadata().excludedCount()).isZero();
        assertThat(result.metadata().droppedCount()).isZero();
        assertThat(result.metadata().truncated()).isFalse();
        assertThat(queryService.requestedLimit).isEqualTo(ConversationContextPolicy.defaults().transcriptLimit());
    }

    @Test
    void assemblerExcludesSensitiveInvisibleDiagnosticAndCurrentRunMessages() {
        RecordingConversationQueryService queryService = new RecordingConversationQueryService(List.of(
                message("m1", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "Keep me"),
                message("m2", "run-1", ConversationMessageRole.TOOL, ConversationMessageStatus.COMPLETED, "Tool output"),
                message("m3", "run-1", ConversationMessageRole.ERROR, ConversationMessageStatus.FAILED, "Failure detail"),
                message("m4", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "secret", true, false, Map.of()),
                message("m5", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "hidden", false, false, Map.of()),
                message("m6", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.FAILED, "stack trace"),
                message("m7", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "  "),
                message("m8", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "diagnostic", true, false, Map.of("kind", "provider-diagnostic")),
                message("m9", "run-current", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "current prompt"),
                message("m10", "run-2", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "Safe answer")));
        ConversationContextAssembler assembler = new ConversationContextAssembler(queryService, ConversationContextPolicy.defaults());

        ConversationContextAssembler.Result result = assembler.assemble(requestContext(), "session-1", "run-current");

        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("Keep me", "Safe answer");
        assertThat(result.metadata().includedCount()).isEqualTo(2);
        assertThat(result.metadata().excludedCount()).isEqualTo(8);
        assertThat(result.metadata().droppedCount()).isZero();
    }

    @Test
    void assemblerExcludesUnsafeRolesStatusesVisibilityRedactionAndCredentialLikeHistory() {
        RecordingConversationQueryService queryService = new RecordingConversationQueryService(List.of(
                message("m1", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "Safe prior question"),
                message("m2", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "Safe prior answer"),
                message("m3", "run-1", ConversationMessageRole.TOOL, ConversationMessageStatus.COMPLETED, "tool says ok"),
                message("m4", "run-1", ConversationMessageRole.ERROR, ConversationMessageStatus.FAILED, "NullPointerException stack"),
                message("m5", "run-1", ConversationMessageRole.ERROR, ConversationMessageStatus.COMPLETED, "provider raw payload", true, false, Map.of("kind", "provider")),
                message("m6", "run-1", ConversationMessageRole.ERROR, ConversationMessageStatus.COMPLETED, "policy audit trail", true, false, Map.of("kind", "audit")),
                message("m7", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.FAILED, "failed user text"),
                message("m8", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.CANCELLED, "cancelled assistant text"),
                message("m9", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "hidden user", false, false, Map.of()),
                message("m10", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "redacted assistant", true, true, Map.of()),
                message("m11", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "api_key=sk-should-not-pass"),
                message("m12", "run-1", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "safe text", true, false, Map.of("authorization", "Bearer secret")),
                message("m13", "run-current", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "current prompt")));
        ConversationContextAssembler assembler = new ConversationContextAssembler(queryService, ConversationContextPolicy.defaults());

        ConversationContextAssembler.Result result = assembler.assemble(requestContext(), "session-1", "run-current");

        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("Safe prior question", "Safe prior answer");
        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .doesNotContain("tool says ok", "NullPointerException stack", "provider raw payload", "policy audit trail",
                        "failed user text", "cancelled assistant text", "hidden user", "redacted assistant",
                        "api_key=sk-should-not-pass", "safe text", "current prompt");
        assertThat(result.metadata().includedCount()).isEqualTo(2);
        assertThat(result.metadata().excludedCount()).isEqualTo(11);
        assertThat(result.metadata().droppedCount()).isZero();
        assertThat(result.metadata().truncated()).isFalse();
    }

    @Test
    void assemblerDropsOlderMessagesForRecentAndCharacterBudgets() {
        RecordingConversationQueryService queryService = new RecordingConversationQueryService(List.of(
                message("m1", "run-1", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "older"),
                message("m2", "run-2", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "middle"),
                message("m3", "run-3", ConversationMessageRole.USER, ConversationMessageStatus.COMPLETED, "new"),
                message("m4", "run-4", ConversationMessageRole.ASSISTANT, ConversationMessageStatus.COMPLETED, "latest")));
        ConversationContextPolicy policy = new ConversationContextPolicy(3, 9, 10);
        ConversationContextAssembler assembler = new ConversationContextAssembler(queryService, policy);

        ConversationContextAssembler.Result result = assembler.assemble(requestContext(), "session-1", null);

        assertThat(result.messages())
                .extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("new", "latest");
        assertThat(result.metadata().includedCount()).isEqualTo(2);
        assertThat(result.metadata().droppedCount()).isEqualTo(2);
        assertThat(result.metadata().excludedCount()).isZero();
        assertThat(result.metadata().resultingCharacters()).isEqualTo(9);
        assertThat(result.metadata().truncated()).isTrue();
    }

    private static ConversationMessageDto message(String messageId,
                                                  String runId,
                                                  ConversationMessageRole role,
                                                  ConversationMessageStatus status,
                                                  String text) {
        return message(messageId, runId, role, status, text, true, false, Map.of());
    }

    private static ConversationMessageDto message(String messageId,
                                                  String runId,
                                                  ConversationMessageRole role,
                                                  ConversationMessageStatus status,
                                                  String text,
                                                  boolean visible,
                                                  boolean redacted,
                                                  Map<String, Object> metadata) {
        return new ConversationMessageDto(
                messageId,
                "session-1",
                runId,
                null,
                role,
                text,
                status,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:01Z"),
                null,
                null,
                metadata,
                visible,
                redacted);
    }

    private static RequestContext requestContext() {
        return new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of("USER")),
                new CorrelationContext("trace-1", "correlation-1", null));
    }

    private static final class RecordingConversationQueryService implements ConversationQueryService {
        private final List<ConversationMessageDto> messages;
        private int requestedLimit;

        private RecordingConversationQueryService(List<ConversationMessageDto> messages) {
            this.messages = new ArrayList<>(messages);
        }

        @Override
        public PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor) {
            this.requestedLimit = limit;
            return new ConversationTranscriptResponse(sessionId, messages, null, null, null, false, Map.of());
        }
    }
}
