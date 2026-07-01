package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.List;

public interface OpenAiStreamSource {
    Iterable<OpenAiStreamEvent> stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken);
}
