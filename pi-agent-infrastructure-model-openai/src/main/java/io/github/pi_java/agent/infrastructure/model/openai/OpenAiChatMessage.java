package io.github.pi_java.agent.infrastructure.model.openai;

import java.util.Objects;

public record OpenAiChatMessage(String role, String content) {
    public OpenAiChatMessage {
        role = requireNonBlank(role, "role").toLowerCase();
        content = requireNonBlank(content, "content");
        if (!role.equals("user") && !role.equals("assistant")) {
            throw new IllegalArgumentException("role must be user or assistant");
        }
    }

    public static OpenAiChatMessage user(String content) {
        return new OpenAiChatMessage("user", content);
    }

    public static OpenAiChatMessage assistant(String content) {
        return new OpenAiChatMessage("assistant", content);
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
