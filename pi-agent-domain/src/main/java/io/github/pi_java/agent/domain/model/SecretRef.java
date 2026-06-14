package io.github.pi_java.agent.domain.model;

public record SecretRef(String scheme, String ref) {
    public SecretRef {
        scheme = DomainModelValidation.requireNonBlank(scheme, "scheme");
        ref = DomainModelValidation.requireNonBlank(ref, "ref");
        if (!scheme.matches("[A-Za-z][A-Za-z0-9+.-]*")) {
            throw new IllegalArgumentException("scheme must be a valid reference scheme");
        }
        if (!ref.startsWith(scheme + ":")) {
            throw new IllegalArgumentException("ref must start with scheme prefix");
        }
        if (ref.length() == scheme.length() + 1) {
            throw new IllegalArgumentException("ref target must not be blank");
        }
    }

    public static SecretRef of(String ref) {
        String validatedRef = DomainModelValidation.requireNonBlank(ref, "ref");
        int separator = validatedRef.indexOf(':');
        if (separator <= 0 || separator != validatedRef.lastIndexOf(':') && !validatedRef.substring(0, separator).equals("config")) {
            throw new IllegalArgumentException("ref must use scheme:target syntax");
        }
        return new SecretRef(validatedRef.substring(0, separator), validatedRef);
    }

    public String redacted() {
        return scheme + ":***";
    }

    @Override
    public String toString() {
        return "SecretRef[ref=" + redacted() + "]";
    }
}
