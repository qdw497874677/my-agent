package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.runtime.CancellationToken;

public interface OpenAiStreamSource {
    Iterable<OpenAiStreamEvent> stream(String prompt, CancellationToken cancellationToken);
}
