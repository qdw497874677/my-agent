package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import io.github.pi_java.agent.adapter.web.sse.SseRunEventFanout;
import io.github.pi_java.agent.adapter.web.sse.SseSubscription;
import io.github.pi_java.agent.client.event.RunEventDto;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Bridges run-event fanout callbacks into Vaadin Push-safe UI mutations. */
public final class ConsoleLiveRunEventSubscriber implements AutoCloseable {

    private final SseRunEventFanout fanout;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private SseSubscription subscription;
    private Registration detachRegistration;

    public ConsoleLiveRunEventSubscriber(SseRunEventFanout fanout) {
        this.fanout = fanout;
    }

    public boolean available() {
        return fanout != null;
    }

    public void subscribe(Component owner, String runId, Consumer<RunEventDto> handler) {
        if (!available()) {
            return;
        }
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        String cleanRunId = requireText(runId, "runId");
        UI ui = owner.getUI().orElseGet(UI::getCurrent);
        if (ui == null) {
            return;
        }
        close();
        closed.set(false);
        detachRegistration = owner.addDetachListener(event -> close());
        subscription = fanout.subscribe(cleanRunId, event -> ui.access(() -> {
            if (closed.get()) {
                return;
            }
            handler.accept(event);
            if (isTerminal(event)) {
                close();
            }
        }));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        SseSubscription currentSubscription = subscription;
        subscription = null;
        if (currentSubscription != null) {
            currentSubscription.close();
        }
        Registration currentDetachRegistration = detachRegistration;
        detachRegistration = null;
        if (currentDetachRegistration != null) {
            currentDetachRegistration.remove();
        }
    }

    private static boolean isTerminal(RunEventDto event) {
        if (event == null || event.type() == null) {
            return false;
        }
        String type = event.type().toLowerCase(Locale.ROOT);
        return type.contains("run.completed")
                || type.contains("run.failed")
                || type.contains("run.cancelled")
                || type.contains("timed_out");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
