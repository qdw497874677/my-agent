package io.github.pi_java.agent.adapter.web.sse;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SseSubscription implements AutoCloseable {

    private final String runId;
    private final String subscriberId;
    private final Runnable unsubscribe;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SseSubscription(String runId, String subscriberId, Runnable unsubscribe) {
        this.runId = requireText(runId, "runId");
        this.subscriberId = requireText(subscriberId, "subscriberId");
        this.unsubscribe = Objects.requireNonNull(unsubscribe, "unsubscribe must not be null");
    }

    public String runId() {
        return runId;
    }

    public String subscriberId() {
        return subscriberId;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            unsubscribe.run();
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }
}
