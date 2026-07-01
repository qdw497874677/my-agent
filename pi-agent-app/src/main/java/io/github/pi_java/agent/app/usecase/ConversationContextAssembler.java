package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.domain.session.SessionEntryPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * App-layer assembler that turns a selected-session typed transcript into
 * bounded, safe model-visible {@link SessionEntryPayload.MessageEntry}
 * history.
 *
 * <p>This class is the Phase 19 context business-rule seam. It reads history
 * only through {@link ConversationQueryService}, accepts only prior visible
 * user/assistant transcript messages, drops sensitive diagnostics instead of
 * redacting them into prompts, and applies recent-message and character
 * budgets by removing older context first while preserving included
 * chronological order.
 */
public final class ConversationContextAssembler {

    private static final Set<String> DIAGNOSTIC_METADATA_MARKERS = Set.of(
            "audit", "provider", "credential", "approval", "policy", "diagnostic", "tool", "error");

    private final ConversationQueryService conversationQueryService;
    private final ConversationContextPolicy policy;

    public ConversationContextAssembler(ConversationQueryService conversationQueryService,
                                        ConversationContextPolicy policy) {
        this.conversationQueryService = Objects.requireNonNull(conversationQueryService,
                "conversationQueryService must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public Result assemble(RequestContext context, String sessionId, String currentRunId) {
        Objects.requireNonNull(context, "context must not be null");
        requireText(sessionId, "sessionId");

        ConversationTranscriptResponse transcript = conversationQueryService.getTranscript(
                context,
                sessionId,
                policy.transcriptLimit(),
                null);

        List<ConversationMessageDto> eligible = new ArrayList<>();
        int excluded = 0;
        for (ConversationMessageDto message : transcript.messages()) {
            if (isEligible(message, currentRunId)) {
                eligible.add(message);
            } else {
                excluded++;
            }
        }

        int originalEligible = eligible.size();
        List<ConversationMessageDto> bounded = keepNewestByMessageBudget(eligible);
        bounded = keepNewestByCharacterBudget(bounded);

        List<SessionEntryPayload.MessageEntry> messages = new ArrayList<>();
        int resultingCharacters = 0;
        for (ConversationMessageDto message : bounded) {
            String text = message.text().trim();
            resultingCharacters += text.length();
            messages.add(new SessionEntryPayload.MessageEntry(message.role().wireValue(), text));
        }

        int dropped = originalEligible - bounded.size();
        ConversationContextMetadata metadata = new ConversationContextMetadata(
                messages.size(),
                dropped,
                excluded,
                policy.maxTotalCharacters(),
                resultingCharacters,
                dropped > 0);
        return new Result(messages, metadata);
    }

    private List<ConversationMessageDto> keepNewestByMessageBudget(List<ConversationMessageDto> eligible) {
        if (eligible.size() <= policy.maxRecentMessages()) {
            return new ArrayList<>(eligible);
        }
        return new ArrayList<>(eligible.subList(eligible.size() - policy.maxRecentMessages(), eligible.size()));
    }

    private List<ConversationMessageDto> keepNewestByCharacterBudget(List<ConversationMessageDto> candidates) {
        List<ConversationMessageDto> bounded = new ArrayList<>(candidates);
        while (totalCharacters(bounded) > policy.maxTotalCharacters() && !bounded.isEmpty()) {
            bounded.removeFirst();
        }
        return bounded;
    }

    private static int totalCharacters(List<ConversationMessageDto> messages) {
        int total = 0;
        for (ConversationMessageDto message : messages) {
            total += message.text().trim().length();
        }
        return total;
    }

    private static boolean isEligible(ConversationMessageDto message, String currentRunId) {
        if (message == null) {
            return false;
        }
        if (currentRunId != null && currentRunId.equals(message.runId())) {
            return false;
        }
        if (!message.visible() || message.redacted()) {
            return false;
        }
        if (message.text() == null || message.text().isBlank()) {
            return false;
        }
        if (hasSensitiveText(message.text())) {
            return false;
        }
        if (message.role() == ConversationMessageRole.USER) {
            return message.status() == ConversationMessageStatus.COMPLETED;
        }
        if (message.role() == ConversationMessageRole.ASSISTANT) {
            return isSafeAssistantStatus(message.status()) && !hasDiagnosticMetadata(message.metadata());
        }
        return false;
    }

    private static boolean isSafeAssistantStatus(ConversationMessageStatus status) {
        return status == ConversationMessageStatus.COMPLETED || status == ConversationMessageStatus.PARTIAL;
    }

    private static boolean hasDiagnosticMetadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = lower(entry.getKey());
            String value = lower(String.valueOf(entry.getValue()));
            for (String marker : DIAGNOSTIC_METADATA_MARKERS) {
                if (key.contains(marker) || value.contains(marker)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasSensitiveText(String text) {
        String normalized = lower(text);
        return normalized.contains("[redacted]")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("api_key")
                || normalized.contains("apikey")
                || normalized.contains("credential");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }

    public record Result(List<SessionEntryPayload.MessageEntry> messages,
                         ConversationContextMetadata metadata) {

        public Result {
            messages = messages == null ? List.of() : List.copyOf(messages);
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        }
    }
}
