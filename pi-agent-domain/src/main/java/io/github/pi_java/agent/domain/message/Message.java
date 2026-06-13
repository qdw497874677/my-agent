package io.github.pi_java.agent.domain.message;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record Message(
        String messageId,
        SessionId sessionId,
        RunId runId,
        String role,
        String content,
        Set<String> artifactIds,
        Set<String> attachmentIds,
        Instant createdAt
) {

    public Message {
        messageId = requireNonBlank(messageId, "messageId");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        role = requireNonBlank(role, "role");
        content = Objects.requireNonNull(content, "content must not be null");
        artifactIds = Set.copyOf(Objects.requireNonNull(artifactIds, "artifactIds must not be null"));
        attachmentIds = Set.copyOf(Objects.requireNonNull(attachmentIds, "attachmentIds must not be null"));
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
