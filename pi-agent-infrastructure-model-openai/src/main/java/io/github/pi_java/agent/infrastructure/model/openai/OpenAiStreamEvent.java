package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.model.ModelUsage;

public sealed interface OpenAiStreamEvent permits OpenAiStreamEvent.Text, OpenAiStreamEvent.ToolCallFragment,
        OpenAiStreamEvent.Usage, OpenAiStreamEvent.Finish, OpenAiStreamEvent.Error, OpenAiStreamEvent.OnSubscribe {

    static Text text(String delta) {
        return new Text(delta);
    }

    static ToolCallFragment toolCall(int index, String id, String name, String argumentsDelta) {
        return new ToolCallFragment(index, id, name, argumentsDelta);
    }

    static Usage usage(ModelUsage usage) {
        return new Usage(usage);
    }

    static Finish finish(String reason, ModelUsage usage) {
        return new Finish(reason, usage);
    }

    static Error error(Throwable throwable) {
        return new Error(throwable);
    }

    static OnSubscribe onSubscribe(Runnable action) {
        return new OnSubscribe(action);
    }

    record Text(String delta) implements OpenAiStreamEvent {
    }

    record ToolCallFragment(int index, String id, String name, String argumentsDelta) implements OpenAiStreamEvent {
    }

    record Usage(ModelUsage usage) implements OpenAiStreamEvent {
    }

    record Finish(String reason, ModelUsage usage) implements OpenAiStreamEvent {
    }

    record Error(Throwable throwable) implements OpenAiStreamEvent {
    }

    record OnSubscribe(Runnable action) implements OpenAiStreamEvent {
    }
}
