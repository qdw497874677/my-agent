package io.github.pi_java.agent.domain.tool;

final class ToolValidation {
    private ToolValidation() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    static void requireNonBlankIfPresent(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when present");
        }
    }
}
