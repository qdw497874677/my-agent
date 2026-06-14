package io.github.pi_java.agent.adapter.web.sse;

import io.github.pi_java.agent.adapter.web.mapper.RunEventDtoMapper;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.infrastructure.event.RunEventFanout;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SseRunEventFanout implements RunEventFanout {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Consumer<RunEventDto>>> subscribersByRunId = new ConcurrentHashMap<>();

    public SseSubscription subscribe(String runId, Consumer<RunEventDto> consumer) {
        requireText(runId, "runId");
        if (consumer == null) {
            throw new IllegalArgumentException("consumer must not be null");
        }
        String subscriberId = UUID.randomUUID().toString();
        subscribersByRunId.computeIfAbsent(runId, ignored -> new ConcurrentHashMap<>()).put(subscriberId, consumer);
        return new SseSubscription(runId, subscriberId, () -> unsubscribe(runId, subscriberId));
    }

    public void unsubscribe(String runId, String subscriberId) {
        ConcurrentHashMap<String, Consumer<RunEventDto>> subscribers = subscribersByRunId.get(runId);
        if (subscribers == null) {
            return;
        }
        subscribers.remove(subscriberId);
        if (subscribers.isEmpty()) {
            subscribersByRunId.remove(runId, subscribers);
        }
    }

    @Override
    public void publish(RunEvent event) {
        String runId = event.runId().value();
        ConcurrentHashMap<String, Consumer<RunEventDto>> subscribers = subscribersByRunId.get(runId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        RunEventDto dto = RunEventDtoMapper.toDto(event);
        for (Map.Entry<String, Consumer<RunEventDto>> subscriber : subscribers.entrySet()) {
            try {
                subscriber.getValue().accept(dto);
            } catch (RuntimeException ex) {
                unsubscribe(runId, subscriber.getKey());
            }
        }
    }

    public int subscriberCount(String runId) {
        ConcurrentHashMap<String, Consumer<RunEventDto>> subscribers = subscribersByRunId.get(runId);
        return subscribers == null ? 0 : subscribers.size();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }
}
