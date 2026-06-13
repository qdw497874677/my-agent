package io.github.pi_java.agent.domain.runtime;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class CancellationToken {
    private final AtomicReference<String> reason = new AtomicReference<>();

    public void cancel(String reason) {
        String normalizedReason = reason == null || reason.isBlank() ? "cancelled" : reason;
        this.reason.compareAndSet(null, normalizedReason);
    }

    public boolean isCancellationRequested() {
        return reason.get() != null;
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason.get());
    }
}
