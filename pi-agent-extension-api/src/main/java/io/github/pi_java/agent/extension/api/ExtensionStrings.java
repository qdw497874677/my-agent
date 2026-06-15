package io.github.pi_java.agent.extension.api;

final class ExtensionStrings {

    private ExtensionStrings() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
