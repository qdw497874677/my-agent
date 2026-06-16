package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class InMemoryPluginStateStore implements PluginStateStore {

    private static final Pattern TOKEN_ASSIGNMENT = Pattern.compile("(?i)(token|secret|password|apikey|api_key)=\\S+");
    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("/(?:[A-Za-z0-9._-]+/)+[A-Za-z0-9._-]+(?:\\.[A-Za-z0-9._-]+)?");

    private final Clock clock;
    private final Map<String, PluginStateRecord> states = new ConcurrentHashMap<>();

    public InMemoryPluginStateStore() {
        this(Clock.systemUTC());
    }

    public InMemoryPluginStateStore(Clock clock) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<PluginStateRecord> state(String pluginId) {
        return Optional.ofNullable(states.get(requireNonBlank(pluginId, "pluginId")));
    }

    @Override
    public PluginStateRecord disable(String pluginId, String actor, String reason) {
        return put(pluginId, ExtensionLifecycleState.DISABLED, actor, reason);
    }

    @Override
    public PluginStateRecord quarantine(String pluginId, String actor, String reason) {
        return put(pluginId, ExtensionLifecycleState.QUARANTINED, actor, reason);
    }

    private PluginStateRecord put(String pluginId, ExtensionLifecycleState state, String actor, String reason) {
        PluginStateRecord record = new PluginStateRecord(requireNonBlank(pluginId, "pluginId"), state,
                sanitize(actor), sanitize(reason), Instant.now(clock));
        states.put(record.pluginId(), record);
        return record;
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String redacted = TOKEN_ASSIGNMENT.matcher(value).replaceAll("$1=<redacted>");
        return UNIX_ABSOLUTE_PATH.matcher(redacted).replaceAll("<redacted-path>");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
