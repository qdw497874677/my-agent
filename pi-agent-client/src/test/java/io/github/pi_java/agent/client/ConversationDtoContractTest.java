package io.github.pi_java.agent.client;

import io.github.pi_java.agent.client.conversation.ConversationMessageDto;
import io.github.pi_java.agent.client.conversation.ConversationMessageRole;
import io.github.pi_java.agent.client.conversation.ConversationMessageStatus;
import io.github.pi_java.agent.client.conversation.ConversationTranscriptResponse;
import io.github.pi_java.agent.client.conversation.SessionSummaryDto;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden contract test for the Phase 16 conversation read-model DTOs.
 *
 * <p>Locks the public record schemas defined by Phase 16 decisions D-01, D-04,
 * D-06, D-07, D-08, D-13, and D-16 so downstream App/REST/Console clients can
 * compile against a stable typed transcript contract instead of raw
 * {@code List<Map<String,Object>>} session history.
 */
class ConversationDtoContractTest {

    @Test
    void sessionSummaryDtoRecordComponentsAreExactlySpecified() {
        assertThat(recordComponentNames(SessionSummaryDto.class))
                .containsExactly(
                        "sessionId",
                        "title",
                        "status",
                        "lastMessagePreview",
                        "lastActivityAt",
                        "createdAt",
                        "activeRunId",
                        "activeRunStatus",
                        "metadata");
    }

    @Test
    void conversationMessageDtoRecordComponentsAreExactlySpecified() {
        assertThat(recordComponentNames(ConversationMessageDto.class))
                .containsExactly(
                        "messageId",
                        "sessionId",
                        "runId",
                        "stepId",
                        "role",
                        "text",
                        "status",
                        "createdAt",
                        "updatedAt",
                        "firstSequence",
                        "lastSequence",
                        "metadata",
                        "visible",
                        "redacted");
    }

    @Test
    void conversationTranscriptResponseComponentsAreTypedMessages() throws Exception {
        assertThat(recordComponentNames(ConversationTranscriptResponse.class))
                .containsExactly(
                        "sessionId",
                        "messages",
                        "activeRunId",
                        "activeRunStatus",
                        "nextCursor",
                        "hasMore",
                        "metadata");

        // D-13/D-16: transcript messages must be typed ConversationMessageDto records,
        // not raw List<Map<String,Object>> history entries.
        RecordComponent messagesComponent = ConversationTranscriptResponse.class
                .getRecordComponents()[indexOf(ConversationTranscriptResponse.class, "messages")];
        Type messageType = messagesComponent.getGenericType();
        assertThat(messageType).isInstanceOf(ParameterizedType.class);
        ParameterizedType parameterized = (ParameterizedType) messageType;
        assertThat(parameterized.getRawType()).isEqualTo(List.class);
        assertThat(parameterized.getActualTypeArguments()[0])
                .as("transcript messages must be typed ConversationMessageDto, not raw maps")
                .isEqualTo(ConversationMessageDto.class);

        // Sanity assertion that the accessor does not hand back a raw map list type.
        List<ConversationMessageDto> sampleMessages = List.of(
                new ConversationMessageDto(
                        "msg-1", "session-1", "run-1", "step-1",
                        ConversationMessageRole.USER, "hi",
                        ConversationMessageStatus.COMPLETED,
                        Instant.parse("2026-06-28T00:00:00Z"),
                        Instant.parse("2026-06-28T00:00:00Z"),
                        1L, 1L,
                        Map.of("source", "contract-test"),
                        true, false));
        ConversationTranscriptResponse response = new ConversationTranscriptResponse(
                "session-1",
                sampleMessages,
                null,
                null,
                null,
                false,
                null);

        assertThat(response.messages()).containsExactlyElementsOf(sampleMessages);
        assertThat(response.messages().get(0)).isInstanceOf(ConversationMessageDto.class);
    }

    @Test
    void conversationMessageRoleExposesStableLowercaseWireValues() {
        Set<ConversationMessageRole> roles = EnumSet.allOf(ConversationMessageRole.class);
        assertThat(roles).extracting(Enum::name)
                .containsExactlyInAnyOrder("USER", "ASSISTANT", "TOOL", "ERROR");

        Map<ConversationMessageRole, String> wireByRole = roles.stream()
                .collect(Collectors.toMap(r -> r, ConversationMessageRole::wireValue));
        assertThat(wireByRole)
                .containsEntry(ConversationMessageRole.USER, "user")
                .containsEntry(ConversationMessageRole.ASSISTANT, "assistant")
                .containsEntry(ConversationMessageRole.TOOL, "tool")
                .containsEntry(ConversationMessageRole.ERROR, "error");
    }

    @Test
    void conversationMessageStatusExposesStableLowercaseWireValues() {
        Set<ConversationMessageStatus> statuses = EnumSet.allOf(ConversationMessageStatus.class);
        assertThat(statuses).extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "PENDING", "COMPLETED", "FAILED", "CANCELLED", "PARTIAL");

        Map<ConversationMessageStatus, String> wireByStatus = statuses.stream()
                .collect(Collectors.toMap(s -> s, ConversationMessageStatus::wireValue));
        assertThat(wireByStatus)
                .containsEntry(ConversationMessageStatus.PENDING, "pending")
                .containsEntry(ConversationMessageStatus.COMPLETED, "completed")
                .containsEntry(ConversationMessageStatus.FAILED, "failed")
                .containsEntry(ConversationMessageStatus.CANCELLED, "cancelled")
                .containsEntry(ConversationMessageStatus.PARTIAL, "partial");
    }

    @Test
    void dtoConstructorsNormalizeNullCollectionsAndCopyMutablyProvidedOnesImmutably() {
        // Null lists/maps normalize to empty immutable values.
        ConversationTranscriptResponse emptyTranscript = new ConversationTranscriptResponse(
                "session-1", null, null, null, null, false, null);
        assertThat(emptyTranscript.messages()).isEmpty();
        assertThat(emptyTranscript.metadata()).isEmpty();
        assertThatThrownOnMutation(emptyTranscript.messages());
        assertThatThrownOnMutation(emptyTranscript.metadata());

        ConversationMessageDto emptyMessage = new ConversationMessageDto(
                "msg-1", "session-1", "run-1", "step-1",
                ConversationMessageRole.ASSISTANT, null,
                ConversationMessageStatus.COMPLETED,
                Instant.parse("2026-06-28T00:00:00Z"),
                Instant.parse("2026-06-28T00:00:00Z"),
                1L, 1L,
                null, true, false);
        assertThat(emptyMessage.metadata()).isEmpty();
        assertThatThrownOnMutation(emptyMessage.metadata());

        SessionSummaryDto emptySummary = new SessionSummaryDto(
                "session-1", "title", "idle", "preview",
                Instant.parse("2026-06-28T00:00:00Z"),
                Instant.parse("2026-06-28T00:00:00Z"),
                null, null, null);
        assertThat(emptySummary.metadata()).isEmpty();

        // Non-null mutable inputs are defensively copied.
        List<ConversationMessageDto> mutableMessages = new ArrayList<>();
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("k", "v");

        ConversationTranscriptResponse transcript = new ConversationTranscriptResponse(
                "session-1", mutableMessages, "run-1", "RUNNING",
                "cursor-1", true, mutableMetadata);
        mutableMessages.add(new ConversationMessageDto(
                "msg-x", "session-1", "run-1", "step-1",
                ConversationMessageRole.USER, "later",
                ConversationMessageStatus.COMPLETED,
                Instant.parse("2026-06-28T00:00:00Z"),
                Instant.parse("2026-06-28T00:00:00Z"),
                0L, 0L, Map.of(), true, false));
        mutableMetadata.put("k2", "v2");

        assertThat(transcript.messages()).isEmpty();
        assertThat(transcript.metadata()).containsOnlyKeys("k");
        assertThatThrownOnMutation(transcript.messages());
        assertThatThrownOnMutation(transcript.metadata());
    }

    @Test
    void conversationDtosDoNotImportDomainOrFrameworkTypes() throws Exception {
        List<Path> dtoSources = List.of(
                Path.of("src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java"),
                Path.of("src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java"));

        for (Path source : dtoSources) {
            String content = Files.readString(source);
            assertThat(content)
                    .as(source.toString())
                    .doesNotContain("io.github.pi_java.agent.domain")
                    .doesNotContain("org.springframework")
                    .doesNotContain("com.fasterxml.jackson")
                    .doesNotContain("jakarta.")
                    .doesNotContain("com.vaadin")
                    .doesNotContain("io.github.pi_java.agent.infrastructure");
        }
    }

    private static List<String> recordComponentNames(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
    }

    private static int indexOf(Class<?> recordType, String componentName) {
        RecordComponent[] components = recordType.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i].getName().equals(componentName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("No record component named " + componentName);
    }

    private static void assertThatThrownOnMutation(Object collection) {
        try {
            if (collection instanceof java.util.Collection<?> c) {
                c.add(null);
            } else if (collection instanceof java.util.Map<?, ?> m) {
                //noinspection unchecked
                ((Map<Object, Object>) m).put(new Object(), new Object());
            } else {
                throw new AssertionError("Not a mutable collection type: " + collection);
            }
            throw new AssertionError("Expected mutation to fail for " + collection);
        } catch (UnsupportedOperationException expected) {
            // immutable copy, as required
        }
    }
}
