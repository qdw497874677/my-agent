package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.model.SecretRef;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EnvironmentAndPropertySecretResolver implements SecretResolver {
    private static final String ENV_SCHEME = "env";
    private static final String CONFIG_SCHEME = "config";

    private final Map<String, String> environment;
    private final Map<String, String> properties;

    public EnvironmentAndPropertySecretResolver(Map<String, String> environment, Map<String, String> properties) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment must not be null"));
        this.properties = Map.copyOf(Objects.requireNonNull(properties, "properties must not be null"));
    }

    @Override
    public Optional<ResolvedSecret> resolve(SecretRef secretRef) {
        Objects.requireNonNull(secretRef, "secretRef must not be null");
        return switch (secretRef.scheme()) {
            case ENV_SCHEME -> resolveValue(secretRef, environment.get(target(secretRef)));
            case CONFIG_SCHEME -> resolveValue(secretRef, properties.get(target(secretRef)));
            default -> Optional.empty();
        };
    }

    private Optional<ResolvedSecret> resolveValue(SecretRef secretRef, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ResolvedSecret.sensitive(secretRef, rawValue));
    }

    private String target(SecretRef secretRef) {
        return secretRef.ref().substring(secretRef.scheme().length() + 1);
    }

    @Override
    public String toString() {
        return "EnvironmentAndPropertySecretResolver[schemes=env,config]";
    }
}
