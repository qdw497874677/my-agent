package io.github.pi_java.agent.extension.api;

public enum ExtensionLifecycleState {
    DISCOVERED(false),
    LOADED(false),
    STARTED(true),
    DISABLED(false),
    FAILED(false),
    QUARANTINED(false);

    private final boolean available;

    ExtensionLifecycleState(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}
