package io.github.pi_java.agent.infrastructure.event;

import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.domain.event.EventSink;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.event.RunEventType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

public class PersistingEventSink implements EventSink {

    private final TransactionTemplate tx;
    private final RunEventStore eventStore;
    private final RunProjectionRepository runProjectionRepository;
    private final RunEventFanout fanout;

    public PersistingEventSink(
            TransactionTemplate tx,
            RunEventStore eventStore,
            RunProjectionRepository runProjectionRepository,
            RunEventFanout fanout) {
        this.tx = tx;
        this.eventStore = eventStore;
        this.runProjectionRepository = runProjectionRepository;
        this.fanout = fanout;
    }

    @Override
    public void publish(RunEvent event) {
        tx.executeWithoutResult(status -> {
            eventStore.append(event);
            runProjectionRepository.updateLastEventSequence(event.runId().value(), event.sequence(), event.timestamp());
            if (isTerminal(event)) {
                runProjectionRepository.markTerminalIfNotTerminal(
                        event.runId().value(),
                        terminalStatus(event),
                        terminalResult(event),
                        failure(event),
                        event.timestamp());
            }
        });
        fanout.publish(event);
    }

    private static boolean isTerminal(RunEvent event) {
        return event.type() == RunEventType.RUN_COMPLETED
                || event.type() == RunEventType.RUN_FAILED
                || event.type() == RunEventType.RUN_CANCELLED
                || event.type() == RunEventType.RUN_POLICY_BLOCKED;
    }

    private static String terminalStatus(RunEvent event) {
        return switch (event.type()) {
            case RUN_COMPLETED -> "COMPLETED";
            case RUN_FAILED -> "FAILED";
            case RUN_CANCELLED -> "CANCELLED";
            case RUN_POLICY_BLOCKED -> "POLICY_BLOCKED";
            default -> throw new IllegalArgumentException("Event is not terminal: " + event.type());
        };
    }

    private static Map<String, Object> terminalResult(RunEvent event) {
        if (event.type() != RunEventType.RUN_COMPLETED) {
            return null;
        }
        return Map.of("eventId", event.eventId(), "sequence", event.sequence());
    }

    private static Map<String, Object> failure(RunEvent event) {
        if (event.type() == RunEventType.RUN_FAILED || event.type() == RunEventType.RUN_POLICY_BLOCKED) {
            return Map.of("eventId", event.eventId(), "type", event.type().wireName(), "payload", payloadSummary(event.payload()));
        }
        return null;
    }

    private static String payloadSummary(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.schema();
        }
        return payload.getClass().getSimpleName();
    }
}
