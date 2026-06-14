package io.github.pi_java.agent.app.port.model;

import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.SecretRef;

import java.util.Objects;
import java.util.Optional;

public interface SecretResolver {

    Optional<ResolvedSecret> resolve(SecretRef secretRef);

    default ResolvedSecret resolve(CredentialRef credentialRef) {
        Objects.requireNonNull(credentialRef, "credentialRef must not be null");
        return resolve(credentialRef.secretRef())
                .orElseThrow(() -> new SecretResolutionException(credentialRef.secretRef()));
    }

    final class SecretResolutionException extends RuntimeException {
        public SecretResolutionException(SecretRef secretRef) {
            super("Secret could not be resolved for " + Objects.requireNonNull(secretRef, "secretRef must not be null").redacted());
        }
    }
}
