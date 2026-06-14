package io.github.pi_java.agent.domain.model;

import java.util.Objects;

public record CredentialRef(SecretRef secretRef) {
    public CredentialRef {
        Objects.requireNonNull(secretRef, "secretRef must not be null");
    }

    public static CredentialRef of(String ref) {
        return new CredentialRef(SecretRef.of(ref));
    }

    public String scheme() {
        return secretRef.scheme();
    }

    public String ref() {
        return secretRef.ref();
    }

    public String redacted() {
        return secretRef.redacted();
    }

    @Override
    public String toString() {
        return "CredentialRef[ref=" + redacted() + "]";
    }
}
