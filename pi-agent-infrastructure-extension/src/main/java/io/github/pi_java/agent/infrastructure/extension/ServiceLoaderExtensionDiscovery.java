package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public final class ServiceLoaderExtensionDiscovery {

    private final ClassLoader classLoader;

    public ServiceLoaderExtensionDiscovery() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public List<DiscoveredSource> discover(List<ExtensionSource> additionalSources) {
        Objects.requireNonNull(additionalSources, "additionalSources must not be null");
        List<DiscoveredSource> sources = new ArrayList<>(discover());
        additionalSources.stream()
                .map(DiscoveredSource::discovered)
                .forEach(sources::add);
        sources.sort(Comparator
                .comparingInt(DiscoveredSource::order)
                .thenComparing(DiscoveredSource::sourceId));
        return List.copyOf(sources);
    }

    public ServiceLoaderExtensionDiscovery(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    public List<DiscoveredSource> discover() {
        ServiceLoader<ExtensionSource> loader = ServiceLoader.load(ExtensionSource.class, classLoader);
        List<DiscoveredSource> sources = new ArrayList<>();
        var iterator = loader.iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                ExtensionSource source = iterator.next();
                sources.add(DiscoveredSource.discovered(source));
            } catch (ServiceConfigurationError | RuntimeException ex) {
                sources.add(DiscoveredSource.failed(safeIdentifier(ex), sanitize(ex)));
            }
        }
        sources.sort(Comparator
                .comparingInt(DiscoveredSource::order)
                .thenComparing(DiscoveredSource::sourceId));
        return List.copyOf(sources);
    }

    private static String safeIdentifier(Throwable ex) {
        return ex.getClass().getSimpleName() + ":" + Integer.toHexString(Objects.toString(ex.getMessage(), "").hashCode());
    }

    static String sanitize(Throwable ex) {
        String message = Objects.toString(ex.getMessage(), "");
        message = message.replaceAll("(?i)(token|secret|password|key)=[^\\s,;]+", "$1=<redacted>");
        return ex.getClass().getSimpleName() + (message.isBlank() ? "" : ": " + message);
    }

    public enum DiscoveryStatus {
        DISCOVERED,
        FAILED
    }

    public record DiscoveredSource(
            String sourceId,
            DiscoveryStatus status,
            Optional<ExtensionSource> source,
            String redactedError
    ) {
        public DiscoveredSource {
            sourceId = requireNonBlank(sourceId, "sourceId");
            Objects.requireNonNull(status, "status must not be null");
            source = Objects.requireNonNull(source, "source must not be null");
            redactedError = redactedError == null ? "" : redactedError;
        }

        static DiscoveredSource discovered(ExtensionSource source) {
            Objects.requireNonNull(source, "source must not be null");
            ExtensionMetadata metadata = source.metadata();
            return new DiscoveredSource(metadata.extensionId(), DiscoveryStatus.DISCOVERED, Optional.of(source), "");
        }

        static DiscoveredSource failed(String sourceId, String redactedError) {
            return new DiscoveredSource(sourceId, DiscoveryStatus.FAILED, Optional.empty(), redactedError);
        }

        int order() {
            return source.flatMap(value -> optionalOrder(value.metadata().redactedMetadata().get("order")))
                    .orElse(Integer.MAX_VALUE);
        }

        private static Optional<Integer> optionalOrder(Object value) {
            if (value instanceof Number number) {
                return Optional.of(number.intValue());
            }
            if (value instanceof String string && !string.isBlank()) {
                try {
                    return Optional.of(Integer.parseInt(string));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }

        private static String requireNonBlank(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return value;
        }
    }
}
