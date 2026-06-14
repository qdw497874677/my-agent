package io.github.pi_java.agent.adapter.web.sse;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RunEventDto;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RunEventStreamService {

    private static final int REPLAY_LIMIT = 500;
    private static final Set<String> TERMINAL_EVENT_TYPES = Set.of(
            "run.completed",
            "run.failed",
            "run.cancelled",
            "run.policy_blocked");

    private final RunQueryService runQueryService;
    private final SseRunEventFanout fanout;
    private final long timeoutMillis;

    public RunEventStreamService(
            RunQueryService runQueryService,
            SseRunEventFanout fanout,
            @Value("${pi.sse.timeout-millis:300000}") long timeoutMillis) {
        this.runQueryService = runQueryService;
        this.fanout = fanout;
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter replayThenSubscribe(RequestContext context, String sessionId, String runId, long afterSequence, String lastEventId) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        long cursor = resolveCursor(afterSequence, lastEventId);

        EventHistoryResponse replay = runQueryService.listEvents(context, sessionId, runId, cursor, REPLAY_LIMIT);
        for (RunEventDto event : replay.events()) {
            if (!sendEvent(emitter, event, null)) {
                return emitter;
            }
            if (isTerminal(event)) {
                emitter.complete();
                return emitter;
            }
        }

        final SseSubscription[] subscriptionRef = new SseSubscription[1];
        SseSubscription subscription = fanout.subscribe(runId, event -> {
            if (!sendEvent(emitter, event, subscriptionRef[0])) {
                return;
            }
            if (isTerminal(event)) {
                close(subscriptionRef[0]);
                emitter.complete();
            }
        });
        subscriptionRef[0] = subscription;

        emitter.onCompletion(subscription::close);
        emitter.onTimeout(() -> {
            subscription.close();
            emitter.complete();
        });
        emitter.onError(error -> subscription.close());
        return emitter;
    }

    private static long resolveCursor(long afterSequence, String lastEventId) {
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                long parsed = Long.parseLong(lastEventId.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Fall back to afterSequence.
            }
        }
        return Math.max(0L, afterSequence);
    }

    private static boolean sendEvent(SseEmitter emitter, RunEventDto event, SseSubscription subscription) {
        try {
            emitter.send(SseEmitter.event()
                    .id(Long.toString(event.sequence()))
                    .name(event.type())
                    .data(event));
            return true;
        } catch (IOException | IllegalStateException ex) {
            close(subscription);
            emitter.completeWithError(ex);
            return false;
        }
    }

    private static boolean isTerminal(RunEventDto event) {
        return TERMINAL_EVENT_TYPES.contains(event.type());
    }

    private static void close(SseSubscription subscription) {
        if (subscription != null) {
            subscription.close();
        }
    }
}
